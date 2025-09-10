package com.example.nomodel.generationjob.application.dto;

import com.example.nomodel.generationjob.domain.model.GenerationJob;
import com.example.nomodel.generationjob.domain.model.JobStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record JobViewResponse(
        UUID jobId,
        JobStatus status,
        Long inputFileId,
        Long resultFileId,
        String resultFileUrl,
        String inputFileUrl,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static JobViewResponse from(GenerationJob j) {
        return from(j, null, null);
    }

    public static JobViewResponse from(GenerationJob j, String resultFileUrl, String inputFileUrl) {
        return new JobViewResponse(
                j.getId(),
                j.getStatus(),
                j.getInputFileId(),
                j.getResultFileId(),
                resultFileUrl,
                inputFileUrl,
                j.getErrorMessage(),
                j.getCreatedAt(),
                j.getUpdatedAt()
        );
    }
}
