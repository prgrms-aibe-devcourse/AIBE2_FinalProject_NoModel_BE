package com.example.nomodel.model.domain.repository;

import com.example.nomodel.model.domain.model.document.AIModelDocument;

import java.util.List;

/**
 * Elasticsearch 고급 검색 기능을 위한 커스텀 리포지토리 인터페이스
 * search_after를 사용한 효율적인 페이지네이션 지원
 */
public interface AIModelSearchRepositoryCustom {

    /**
     * search_after를 사용한 효율적인 대량 페이지네이션
     * @param keyword 검색 키워드
     * @param searchAfter 이전 페이지의 마지막 문서 정렬 값 (첫 페이지는 null)
     * @param size 페이지 크기
     * @return 검색 결과와 다음 페이지를 위한 search_after 값
     */
    SearchAfterResult searchWithSearchAfter(String keyword, Object[] searchAfter, int size);

    /**
     * search_after 검색 결과를 담는 DTO
     */
    class SearchAfterResult {
        private List<AIModelDocument> documents;
        private Object[] nextSearchAfter;  // 다음 페이지를 위한 정렬 값
        private long totalHits;
        private boolean hasNext;

        public SearchAfterResult(List<AIModelDocument> documents, Object[] nextSearchAfter,
                                long totalHits, boolean hasNext) {
            this.documents = documents;
            this.nextSearchAfter = nextSearchAfter;
            this.totalHits = totalHits;
            this.hasNext = hasNext;
        }

        // Getters
        public List<AIModelDocument> getDocuments() { return documents; }
        public Object[] getNextSearchAfter() { return nextSearchAfter; }
        public long getTotalHits() { return totalHits; }
        public boolean hasNext() { return hasNext; }
    }
}