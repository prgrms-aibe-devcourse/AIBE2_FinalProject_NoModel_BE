package com.example.nomodel.model.command.application.dto;

import lombok.Builder;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 응답 DTO
 * Spring Data의 Page 인터페이스를 안정적인 JSON 구조로 변환
 */
@Builder
public record PageResponse<T>(
        List<T> content,           // 현재 페이지 데이터
        int page,                  // 현재 페이지 번호 (0부터 시작)
        int size,                  // 페이지 크기
        long totalElements,        // 전체 데이터 수
        int totalPages,            // 전체 페이지 수
        boolean first,             // 첫 페이지 여부
        boolean last,              // 마지막 페이지 여부
        boolean empty              // 비어있는 여부
) {
    
    /**
     * Spring Data Page를 PageResponse로 변환
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }
    
    /**
     * 리스트를 단일 페이지 PageResponse로 변환
     */
    public static <T> PageResponse<T> of(List<T> content) {
        return PageResponse.<T>builder()
                .content(content)
                .page(0)
                .size(content.size())
                .totalElements(content.size())
                .totalPages(content.isEmpty() ? 0 : 1)
                .first(true)
                .last(true)
                .empty(content.isEmpty())
                .build();
    }
    
    /**
     * 빈 페이지 응답 생성
     */
    public static <T> PageResponse<T> empty(int page, int size) {
        return PageResponse.<T>builder()
                .content(List.of())
                .page(page)
                .size(size)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .empty(true)
                .build();
    }
}