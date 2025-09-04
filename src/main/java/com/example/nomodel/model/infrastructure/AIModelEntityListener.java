package com.example.nomodel.model.infrastructure;

import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.model.application.service.AIModelSearchService;
import com.example.nomodel.model.domain.model.AIModel;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AI 모델 엔티티 이벤트 리스너
 * JPA 이벤트를 통해 자동으로 Elasticsearch에 인덱싱
 */
@Slf4j
@Component
public class AIModelEntityListener {

    private static AIModelSearchService searchService;
    private static MemberJpaRepository memberRepository;

    @Autowired
    public void setSearchService(AIModelSearchService searchService) {
        AIModelEntityListener.searchService = searchService;
    }

    @Autowired
    public void setMemberRepository(MemberJpaRepository memberRepository) {
        AIModelEntityListener.memberRepository = memberRepository;
    }

    @PostPersist
    @PostUpdate
    public void afterSaveOrUpdate(AIModel aiModel) {
        if (searchService == null) {
            log.warn("AIModelSearchService가 주입되지 않아 Elasticsearch 인덱싱을 건너뜁니다.");
            return;
        }

        try {
            // 소유자 정보 조회
            String ownerName = getOwnerName(aiModel.getOwnerId());
            
            // Elasticsearch에 인덱싱 (비동기로 처리하는 것이 좋지만 일단 동기로)
            searchService.indexModel(aiModel, ownerName);
            
            log.debug("AI 모델 Elasticsearch 인덱싱 완료: modelId={}", aiModel.getId());
            
        } catch (Exception e) {
            log.error("AI 모델 Elasticsearch 인덱싱 실패: modelId={}, error={}", 
                     aiModel.getId(), e.getMessage(), e);
        }
    }

    private String getOwnerName(Long ownerId) {
        if (ownerId == null || memberRepository == null) {
            return "ADMIN";
        }
        
        try {
            return memberRepository.findById(ownerId)
                    .map(member -> member.getEmail().getValue())
                    .orElse("Unknown");
        } catch (Exception e) {
            log.warn("소유자 정보 조회 실패: ownerId={}, error={}", ownerId, e.getMessage());
            return "Unknown";
        }
    }
}