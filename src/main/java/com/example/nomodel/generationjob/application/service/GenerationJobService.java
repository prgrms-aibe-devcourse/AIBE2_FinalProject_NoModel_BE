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
            // (1) 누끼 PNG 로드 (합성 단계에서 사용 예정)
            byte[] cutout = fileService.loadAsBytes(job.getInputFileId()); // 미사용이어도 검증 겸 로드

            // (2) 배경/인물+배경 생성
            Map<String, Object> opts = Map.of("aspect_ratio", "9:16");
            byte[] base = imageGenerator.generate(mode, prompt, opts);

            // (3) 임시: 생성 이미지만 저장
            Long resultId = fileService.saveBytes(
                    base, "image/png", RelationType.AD, job.getInputFileId(), FileType.PREVIEW
            );

            job.succeed(resultId);
            log.info("[JOB] OK COMPOSE id={} -> SUCCEEDED (resultFileId={})", jobId, resultId);
        } catch (Exception e) {
            job.fail(e.getMessage());
            log.error("[JOB] FAIL COMPOSE id={} -> FAILED : {}", jobId, e.getMessage(), e);
        }
        // JPA dirty checking으로 flush
    }

    /**
     * remove-bg 실행 로직 (비동기 워커)
     * - 별도 트랜잭션으로 분리(REQUIRES_NEW): enqueue 트랜잭션과 독립 실행
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runRemoveBg(
            UUID jobId,
            java.util.function.BiFunction<Long, Map<String, Object>, Long> worker,
            Map<String, Object> params
    ) {
        GenerationJob job = repo.findById(jobId).orElseThrow();

        job.markRunning();
        log.info("[JOB] RUN REMOVE_BG id={} -> RUNNING (inputFileId={})", jobId, job.getInputFileId());

        try {
            Long outFileId = worker.apply(job.getInputFileId(), params); // 입력 fileId -> 결과 fileId

            if (outFileId == null) {
                throw new IllegalStateException("BackgroundRemovalService returned null fileId");
            }

            job.succeed(outFileId);
            log.info("[JOB] OK REMOVE_BG id={} -> SUCCEEDED (resultFileId={})", jobId, outFileId);

        } catch (Exception e) {
            job.fail(e.getMessage());
            log.error("[JOB] FAIL REMOVE_BG id={} -> FAILED : {}", jobId, e.getMessage(), e);
        }
        // 종료 시점 flush
    }
}
