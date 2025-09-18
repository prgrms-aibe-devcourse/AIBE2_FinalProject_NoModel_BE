package com.example.nomodel.generationjob.application.service;

import com.example.nomodel.compose.application.service.ImageCompositor;
import com.example.nomodel.file.application.service.FileService;
import com.example.nomodel.file.domain.model.FileType;
import com.example.nomodel.file.domain.model.RelationType;
import com.example.nomodel.generate.application.service.StableDiffusionImageGenerator;
import com.example.nomodel.generationjob.domain.model.GenerationMode;
import com.example.nomodel.generationjob.domain.model.GenerationJob;
import com.example.nomodel.generationjob.domain.repository.GenerationJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationJobService {

    private final GenerationJobRepository repo;
    private final FileService fileService;
    private final StableDiffusionImageGenerator imageGenerator; // replicate/dummy 중 활성 빈
    private final ImageCompositor imageCompositor; // 이미지 합성 서비스

    /**
     * Repository 접근자 (Controller에서 직접 접근시 사용)
     */
    public GenerationJobRepository getRepo() {
        return repo;
    }

    /** remove-bg 잡 큐잉 */
    @Transactional
    public GenerationJob enqueueRemoveBg(Long ownerId, Long fileId, String paramsJson) {
        GenerationJob job = GenerationJob.createRemoveBgJob(ownerId, fileId);
        job.setParameters(paramsJson);
        GenerationJob saved = repo.save(job);
        log.info("[JOB] enqueue REMOVE_BG id={}, owner={}, fileId={}", saved.getId(), ownerId, fileId);
        return saved;
    }

    /** 잡 단건 조회 */
    @Transactional(readOnly = true)
    public GenerationJob view(UUID jobId) {
        return repo.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Job not found: " + jobId));
    }

    /** compose(배경/인물+배경 생성) 잡 큐잉 - 동기 처리 */
    @Transactional
    public String enqueueCompose(Long userId, Long fileId, String prompt, GenerationMode mode) {
        GenerationJob job = GenerationJob.createComposeJob(userId, fileId);
        job.setComposeParams(prompt, mode);
        repo.save(job);

        // 간단 구현: 동기 실행 (비동기화하려면 @Async + 프록시 호출 필요)
        runCompose(job.getId(), prompt, mode);

        log.info("[JOB] enqueue COMPOSE id={}, owner={}, fileId={}, mode={}", job.getId(), userId, fileId, mode);
        return job.getId().toString();
    }

    /** compose(이미지 합성) 잡 큐잉 - 비동기 처리 */
    @Transactional
    public String enqueueComposeAsync(Long userId, Long productFileId, String prompt, GenerationMode mode) {
        GenerationJob job = GenerationJob.createComposeJob(userId, productFileId);
        job.setComposeParams(prompt, mode);
        repo.save(job);

        // 비동기 실행
        runImageCompositionAsync(job.getId(), productFileId, prompt);

        log.info("[JOB] enqueue IMAGE_COMPOSITION ASYNC id={}, owner={}, fileId={}, mode={}", job.getId(), userId, productFileId, mode);
        return job.getId().toString();
    }

    /** 이미지 합성 실행 로직 - 비동기 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runImageCompositionAsync(UUID jobId, Long productFileId, String prompt) {
        GenerationJob job = repo.findById(jobId).orElseThrow();
        job.markRunning();
        repo.save(job); // 상태 변경 저장
        log.info("[JOB] RUN IMAGE_COMPOSITION ASYNC id={} -> RUNNING", jobId);

        try {
            // 제품 이미지와 모델 이미지 로드
            byte[] productImage = fileService.loadAsBytes(productFileId);
            
            // 모델 파일 ID는 Job의 modelId에서 가져옴
            Long modelFileId = job.getModelId();
            if (modelFileId == null) {
                throw new IllegalStateException("Model file ID is not set for job: " + jobId);
            }
            
            byte[] modelImage = fileService.loadAsBytes(modelFileId);
            
            log.info("Retrieved images - Product: {} bytes, Model: {} bytes", 
                    productImage.length, modelImage.length);
            
            // ImageCompositor를 사용하여 이미지 합성
            byte[] compositeResult = imageCompositor.composite(productImage, modelImage, prompt);
            
            log.info("Image composition completed - Result: {} bytes", compositeResult.length);
            
            // 합성 결과를 Firebase에 저장
            Long resultFileId = fileService.saveBytes(
                compositeResult,
                "image/png",
                RelationType.AD, // 광고 관련 타입
                productFileId, // 관련 ID (제품 파일)
                FileType.RESULT // 원래대로 RESULT 사용
            );

            job.succeed(resultFileId);
            repo.save(job); // 성공 상태 저장
            log.info("[JOB] OK IMAGE_COMPOSITION ASYNC id={} -> SUCCEEDED (resultFileId={})", jobId, resultFileId);
        } catch (Exception e) {
            job.fail("Image composition failed: " + e.getMessage());
            repo.save(job); // 실패 상태 저장
            log.error("[JOB] FAIL IMAGE_COMPOSITION ASYNC id={} -> FAILED : {}", jobId, e.getMessage(), e);
        }
    }

    /** compose 실행 로직 - 동기 */
    @Transactional
    public void runCompose(UUID jobId, String prompt, GenerationMode mode) {
        GenerationJob job = repo.findById(jobId).orElseThrow();
        job.markRunning();
        log.info("[JOB] RUN COMPOSE id={} -> RUNNING", jobId);

        try {
            // (1) 누라 PNG 로드 (합성 단계에서 사용 예정)
            byte[] cutout = fileService.loadAsBytes(job.getInputFileId()); // 미사용이어도 검증 겸 로드

            // (2) 배경/인물+배경 생성 - 옵션에 relationId와 relationType 추가
            Map<String, Object> opts = new HashMap<>();
            opts.put("aspect_ratio", "9:16");
            opts.put("relationId", job.getInputFileId()); // 관련 파일 ID 설정
            opts.put("relationType", RelationType.AD.name()); // AD 타입으로 설정
            
            // imageGenerator.generate()가 이제 파일 ID를 반환
            Long resultFileId = imageGenerator.generate(mode, prompt, opts);

            job.succeed(resultFileId);
            log.info("[JOB] OK COMPOSE id={} -> SUCCEEDED (resultFileId={})", jobId, resultFileId);
        } catch (Exception e) {
            job.fail(e.getMessage());
            log.error("[JOB] FAIL COMPOSE id={} -> FAILED : {}", jobId, e.getMessage(), e);
        }
    }

    /** compose 실행 로직 - 비동기 */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runComposeAsync(UUID jobId, String prompt, GenerationMode mode) {
        GenerationJob job = repo.findById(jobId).orElseThrow();
        job.markRunning();
        repo.save(job); // 상태 변경 저장
        log.info("[JOB] RUN COMPOSE ASYNC id={} -> RUNNING", jobId);

        try {
            // (1) 누라 PNG 로드 (합성 단계에서 사용 예정)
            byte[] cutout = fileService.loadAsBytes(job.getInputFileId()); // 미사용이어도 검증 겸 로드

            // (2) 배경/인물+배경 생성 - 옵션에 relationId와 relationType 추가
            Map<String, Object> opts = new HashMap<>();
            opts.put("aspect_ratio", "9:16");
            opts.put("relationId", job.getInputFileId()); // 관련 파일 ID 설정
            opts.put("relationType", RelationType.AD.name()); // AD 타입으로 설정
            
            // imageGenerator.generate()가 이제 파일 ID를 반환
            Long resultFileId = imageGenerator.generate(mode, prompt, opts);

            job.succeed(resultFileId);
            repo.save(job); // 성공 상태 저장
            log.info("[JOB] OK COMPOSE ASYNC id={} -> SUCCEEDED (resultFileId={})", jobId, resultFileId);
        } catch (Exception e) {
            job.fail(e.getMessage());
            repo.save(job); // 실패 상태 저장
            log.error("[JOB] FAIL COMPOSE ASYNC id={} -> FAILED : {}", jobId, e.getMessage(), e);
        }
    }

    /**
     * remove-bg 실행 로직
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runRemoveBg(
            UUID jobId,
            java.util.function.BiFunction<Long, Map<String, Object>, Long> worker,
            Map<String, Object> params
    ) {
        log.info("[JOB] Starting runRemoveBg for jobId: {}", jobId);
        
        GenerationJob job = repo.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Job not found: " + jobId));

        try {
            // 상태를 RUNNING으로 변경하고 즉시 저장
            job.markRunning();
            repo.save(job);
            log.info("[JOB] RUN REMOVE_BG id={} -> RUNNING (inputFileId={})", jobId, job.getInputFileId());

            // 실제 작업 수행
            Long outFileId = worker.apply(job.getInputFileId(), params);

            if (outFileId == null) {
                throw new IllegalStateException("BackgroundRemovalService returned null fileId");
            }

            // 성공 상태로 변경
            job.succeed(outFileId);
            repo.save(job);
            log.info("[JOB] OK REMOVE_BG id={} -> SUCCEEDED (resultFileId={})", jobId, outFileId);

        } catch (Exception e) {
            try {
                // 실패 상태로 변경
                job.fail(e.getMessage());
                repo.save(job);
                log.error("[JOB] FAIL REMOVE_BG id={} -> FAILED : {}", jobId, e.getMessage(), e);
            } catch (Exception saveException) {
                log.error("[JOB] Failed to save job failure state for id={}: {}", jobId, saveException.getMessage(), saveException);
            }
        }
    }

    /**
     * 이미지 생성 잡 큐잉 (새로 추가)
     */
    @Transactional
    public String enqueueImageGeneration(Long userId, String prompt, GenerationMode mode, Map<String, Object> additionalOpts) {
        // 이미지 생성은 입력 파일이 없으므로 null로 설정
        GenerationJob job = GenerationJob.createComposeJob(userId, null);
        job.setComposeParams(prompt, mode);
        repo.save(job);

        // 비동기로 실행할 수도 있음
        runImageGeneration(job.getId(), prompt, mode, additionalOpts);

        log.info("[JOB] enqueue IMAGE_GENERATION id={}, owner={}, mode={}", job.getId(), userId, mode);
        return job.getId().toString();
    }

    /**
     * 이미지 생성 실행 로직 (새로 추가)
     */
    @Transactional
    public void runImageGeneration(UUID jobId, String prompt, GenerationMode mode, Map<String, Object> additionalOpts) {
        GenerationJob job = repo.findById(jobId).orElseThrow();
        job.markRunning();
        log.info("[JOB] RUN IMAGE_GENERATION id={} -> RUNNING", jobId);

        try {
            // 기본 옵션 설정
            Map<String, Object> opts = new HashMap<>();
            if (additionalOpts != null) {
                opts.putAll(additionalOpts);
            }
            
            // 기본값 설정
            opts.putIfAbsent("width", 512);
            opts.putIfAbsent("height", 512);
            opts.putIfAbsent("steps", 25);
            opts.putIfAbsent("cfg_scale", 7.0);
            opts.putIfAbsent("relationId", 0L);
            opts.putIfAbsent("relationType", RelationType.MODEL.name());

            // 이미지 생성 및 저장
            Long resultFileId = imageGenerator.generate(mode, prompt, opts);

            job.succeed(resultFileId);
            log.info("[JOB] OK IMAGE_GENERATION id={} -> SUCCEEDED (resultFileId={})", jobId, resultFileId);
        } catch (Exception e) {
            job.fail(e.getMessage());
            log.error("[JOB] FAIL IMAGE_GENERATION id={} -> FAILED : {}", jobId, e.getMessage(), e);
        }
    }

    /**
     * Job 상태만 업데이트하는 헬퍼 메서드
     */
    @Transactional
    public void updateJobStatus(UUID jobId, String status, String errorMessage) {
        GenerationJob job = repo.findById(jobId).orElseThrow();
        switch (status.toUpperCase()) {
            case "RUNNING" -> job.markRunning();
            case "FAILED" -> job.fail(errorMessage);
            case "SUCCEEDED" -> {
                // 성공시에는 resultFileId가 필요하므로 별도 메서드 사용
                throw new IllegalArgumentException("Use updateJobSuccess method for SUCCEEDED status");
            }
        }
        repo.save(job);
    }

    /**
     * Job 성공 상태 업데이트
     */
    @Transactional
    public void updateJobSuccess(UUID jobId, Long resultFileId) {
        GenerationJob job = repo.findById(jobId).orElseThrow();
        job.succeed(resultFileId);
        repo.save(job);
    }
}
