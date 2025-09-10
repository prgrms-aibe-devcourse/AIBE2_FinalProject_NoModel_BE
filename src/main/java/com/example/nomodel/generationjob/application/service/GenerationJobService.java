package com.example.nomodel.generationjob.application.service;

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

    /** compose(배경/인물+배경 생성) 잡 큐잉 */
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

    /** compose 실행 로직 */
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

    /**
     * remove-bg 실행 로직
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
    public void runRemoveBg(
            UUID jobId,
            java.util.function.BiFunction<Long, Map<String, Object>, Long> worker,
            Map<String, Object> params
    ) {
        GenerationJob job = repo.findById(jobId).orElseThrow();

        job.markRunning();
        log.info("[JOB] RUN REMOVE_BG id={} -> RUNNING (inputFileId={})", jobId, job.getInputFileId());

        try {
            Long outFileId = worker.apply(job.getInputFileId(), params);

            if (outFileId == null) {
                throw new IllegalStateException("BackgroundRemovalService returned null fileId");
            }

            job.succeed(outFileId);
            log.info("[JOB] OK REMOVE_BG id={} -> SUCCEEDED (resultFileId={})", jobId, outFileId);

        } catch (Exception e) {
            job.fail(e.getMessage());
            log.error("[JOB] FAIL REMOVE_BG id={} -> FAILED : {}", jobId, e.getMessage(), e);
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
}