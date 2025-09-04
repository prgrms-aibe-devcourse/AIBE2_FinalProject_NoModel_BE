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
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static JobViewResponse from(GenerationJob j) {
        return new JobViewResponse(
                j.getId(),
                j.getStatus(),
                j.getInputFileId(),
                j.getResultFileId(),
                j.getErrorMessage(),
                j.getCreatedAt(),
                j.getUpdatedAt()
        );
    }
}
