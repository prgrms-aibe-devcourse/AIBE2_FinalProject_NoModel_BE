package com.example.nomodel.model.domain.repository;

import com.example.nomodel.model.application.dto.ModelUsageProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.nomodel.model.domain.model.AdResult;

/**
 * 모델 사용 내역 조회 전용 Repository
 * AdResult, AIModel, File 테이블을 조인하여 모델 사용 내역을 조회합니다.
 */
public interface ModelUsageRepository extends JpaRepository<AdResult, Long> {
    
    /**
     * 회원의 모델 사용 내역을 조인하여 조회 (전체 모델)
     * @param memberId 회원 ID
     * @param pageable 페이징 정보
     * @return 모델 사용 내역 페이지
     */
    @Query("""
        SELECT ar.id as adResultId,
               ar.modelId as modelId,
               m.modelName as modelName,
               f.fileUrl as modelImageUrl,
               ar.prompt as prompt,
               ar.createdAt as createdAt
        FROM AdResult ar
        JOIN AIModel m ON ar.modelId = m.id
        LEFT JOIN File f ON f.relationType = 'MODEL' AND f.relationId = m.id
        WHERE ar.memberId = :memberId
        ORDER BY ar.createdAt DESC
        """)
    Page<ModelUsageProjection> findModelUsageByMemberId(
        @Param("memberId") Long memberId, 
        Pageable pageable
    );
    
    /**
     * 회원의 특정 모델 사용 내역을 조인하여 조회
     * @param memberId 회원 ID
     * @param modelId 모델 ID
     * @param pageable 페이징 정보
     * @return 모델 사용 내역 페이지
     */
    @Query("""
        SELECT ar.id as adResultId,
               ar.modelId as modelId,
               m.modelName as modelName,
               f.fileUrl as modelImageUrl,
               ar.prompt as prompt,
               ar.createdAt as createdAt
        FROM AdResult ar
        JOIN AIModel m ON ar.modelId = m.id
        LEFT JOIN File f ON f.relationType = 'MODEL' AND f.relationId = m.id
        WHERE ar.memberId = :memberId AND ar.modelId = :modelId
        ORDER BY ar.createdAt DESC
        """)
    Page<ModelUsageProjection> findModelUsageByMemberIdAndModelId(
        @Param("memberId") Long memberId,
        @Param("modelId") Long modelId,
        Pageable pageable
    );
    
    /**
     * 회원의 총 모델 사용 횟수 조회
     * @param memberId 회원 ID
     * @return 사용 횟수
     */
    @Query("SELECT COUNT(ar) FROM AdResult ar WHERE ar.memberId = :memberId")
    long countModelUsageByMemberId(@Param("memberId") Long memberId);
}