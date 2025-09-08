package com.example.nomodel.generationjob.application.service;

import com.example.nomodel.file.application.service.FileService;
import com.example.nomodel.file.domain.model.FileType;
import com.example.nomodel.file.domain.model.RelationType;
import com.example.nomodel.generate.application.service.StableDiffusionImageGenerator;
import com.example.nomodel.generationjob.domain.model.GenerationMode;
import com.example.nomodel.generationjob.domain.model.GenerationJob;
import com.example.nomodel.generationjob.domain.model.JobStatus;
import com.example.nomodel.generationjob.domain.model.JobType;
import com.example.nomodel.generationjob.domain.repository.GenerationJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

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
        return repo.save(job);
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

        // 간단 구현: 즉시 실행(동기). 비동기화하려면 runCompose에 @Async 부여 + 프록시 통해 호출 필요.
        runCompose(job.getId(), prompt, mode);

        return job.getId().toString();
    }

    /** compose 실행 로직 */
    @Transactional
    public void runCompose(UUID jobId, String prompt, GenerationMode mode) {
        GenerationJob job = repo.findById(jobId).orElseThrow();
        job.markRunning();
        try {
            // (1) 누끼 PNG 바이트 (다음 단계 인페인팅 합성에 사용 예정)
            byte[] cutout = fileService.loadAsBytes(job.getInputFileId());

            // (2) 배경 or 인물+배경 생성
            Map<String, Object> opts = Map.of(
                    "aspect_ratio", "9:16" // 필요 시 옵션 확장
            );
            byte[] base = imageGenerator.generate(mode, prompt, opts);

            // (3) (임시) 합성 전: 생성 이미지를 그대로 저장
            Long resultId = fileService.saveBytes(
                    base, "image/png", RelationType.AD, job.getInputFileId(), FileType.PREVIEW
            );

            job.succeed(resultId);
        } catch (Exception e) {
            job.fail(e.getMessage());
        }
        // 트랜잭션 종료 시점에 JPA dirty checking으로 flush
    }

    /** remove-bg 실행 로직 (비동기) */
    @Async
    @Transactional
    public void runRemoveBg(
            UUID jobId,
            java.util.function.BiFunction<Long, Map<String, Object>, Long> worker,
            Map<String, Object> params
    ) {
        GenerationJob job = repo.findById(jobId).orElseThrow();
        try {
            job.markRunning();
            Long outFileId = worker.apply(job.getInputFileId(), params); // 입력 fileId -> 결과 fileId
            job.succeed(outFileId);
        } catch (Exception e) {
            job.fail(e.getMessage());
        }
        // 트랜잭션 종료 시점에 저장
    }
}
