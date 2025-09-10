package com.example.nomodel.model.application.dto.response;

import lombok.Builder;
import org.springframework.data.domain.Page;

import java.util.List;

@Builder
public record ModelUsageHistoryPageResponse(
    List<ModelUsageHistoryResponse> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious
) {
    public static ModelUsageHistoryPageResponse from(Page<ModelUsageHistoryResponse> page) {
        return ModelUsageHistoryPageResponse.builder()
            .content(page.getContent())
            .pageNumber(page.getNumber())
            .pageSize(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .hasNext(page.hasNext())
            .hasPrevious(page.hasPrevious())
            .build();
    }
}