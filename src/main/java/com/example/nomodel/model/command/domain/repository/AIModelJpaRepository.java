package com.example.nomodel.model.command.domain.repository;

import com.example.nomodel.model.command.application.dto.ModelWithStatisticsProjection;
import com.example.nomodel.model.command.domain.model.AIModel;
import com.example.nomodel.model.command.domain.model.OwnType;
import com.example.nomodel.model.command.application.dto.response.AdminAIModelResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.nomodel.review.domain.model.ReviewStatus;
import com.example.nomodel.model.command.application.dto.ModelIndexProjection;
import java.util.Optional;

public interface AIModelJpaRepository extends JpaRepository<AIModel, Long> {

    /**
     * 공개된 모델 조회
     */
    @Query("SELECT m FROM AIModel m WHERE m.isPublic = true")
    List<AIModel> findByIsPublicTrue();

    /**
     * 특정 소유자의 모델 조회
     */
    List<AIModel> findByOwnerId(Long ownerId);

    /**
     * 소유 타입별 모델 조회
     */
    List<AIModel> findByOwnType(OwnType ownType);

    /**
     * 공개된 관리자 모델 조회
     */
    @Query("SELECT m FROM AIModel m WHERE m.ownType = :ownType AND m.isPublic = true")
    List<AIModel> findByOwnTypeAndIsPublicTrue(@Param("ownType") OwnType ownType);

    /**
     * 특정 소유자의 공개/비공개 모델 조회
     */
    @Query("SELECT m FROM AIModel m WHERE m.ownerId = :ownerId AND m.isPublic = :isPublic")
    List<AIModel> findByOwnerIdAndIsPublic(@Param("ownerId") Long ownerId, @Param("isPublic") boolean isPublic);

    /**
     * 무료 공개 모델 조회
     */
    @Query("SELECT m FROM AIModel m WHERE m.isPublic = true AND (m.price IS NULL OR m.price = 0)")
    List<AIModel> findFreePublicModels();

    /**
     * 유료 공개 모델 조회
     */
    @Query("SELECT m FROM AIModel m WHERE m.isPublic = true AND m.price > 0")
    List<AIModel> findPaidPublicModels();

    /**
     * 가격 범위별 공개 모델 조회
     */
    @Query("SELECT m FROM AIModel m WHERE m.isPublic = true AND m.price BETWEEN :minPrice AND :maxPrice")
    List<AIModel> findPublicModelsByPriceRange(@Param("minPrice") BigDecimal minPrice, 
                                              @Param("maxPrice") BigDecimal maxPrice);

    /**
     * 모델명으로 검색 (부분 일치)
     */
    List<AIModel> findByModelNameContainingIgnoreCase(String modelName);

    /**
     * 공개 모델 중 모델명으로 검색
     */
    @Query("SELECT m FROM AIModel m WHERE m.isPublic = true AND LOWER(m.modelName) LIKE LOWER(CONCAT('%', :modelName, '%'))")
    List<AIModel> findByIsPublicTrueAndModelNameContainingIgnoreCase(@Param("modelName") String modelName);

    /**
     * ID와 소유자로 모델 조회 (권한 체크용)
     */
    Optional<AIModel> findByIdAndOwnerId(Long id, Long ownerId);

    /**
     * 모델 존재 여부 확인
     */
    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    /**
     * 특정 소유자의 모델 수 카운트
     */
    long countByOwnerId(Long ownerId);

    /**
     * 공개 모델 수 카운트
     */
    @Query("SELECT COUNT(m) FROM AIModel m WHERE m.isPublic = true")
    long countByIsPublicTrue();

    /**
     * 특정 시간 이후 수정된 모델 조회 (배치 증분 처리용)
     * BaseTimeEntity 특성상 생성 시 updatedAt도 설정되므로 새 모델도 포함됨
     */
    @Query("SELECT m FROM AIModel m WHERE m.updatedAt >= :fromDateTime ORDER BY m.updatedAt ASC")
    List<AIModel> findModelsUpdatedAfter(@Param("fromDateTime") LocalDateTime fromDateTime);

    /**
     * 특정 시간 이후 수정된 모델 페이징 조회 (배치 증분 처리용)
     * BaseTimeEntity 특성상 생성 시 updatedAt도 설정되므로 새 모델도 포함됨
     */
    @Query("SELECT m FROM AIModel m WHERE m.updatedAt >= :fromDateTime ORDER BY m.updatedAt ASC")
    org.springframework.data.domain.Page<AIModel> findModelsUpdatedAfterPaged(
            @Param("fromDateTime") LocalDateTime fromDateTime, 
            org.springframework.data.domain.Pageable pageable);

    /**
     * 모든 모델과 통계, 소유자 정보를 한 번에 조회 (하이브리드 최적화)
     * LEFT JOIN으로 통계가 없는 모델도 포함
     * 소유자 정보도 함께 조회하여 N+1 문제 해결
     */
    @Query("""
        SELECT m as model, s as statistics, mem.username as ownerName
        FROM AIModel m
        LEFT JOIN ModelStatistics s ON s.model.id = m.id
        LEFT JOIN Member mem ON mem.id = m.ownerId
        """)
    List<ModelWithStatisticsProjection> findAllModelsWithStatisticsAndOwner();


    @Query("""
        SELECT m as model, s as statistics, mem.username as ownerName,
               (SELECT AVG(r.rating.value) FROM ModelReview r WHERE r.modelId = m.id AND r.status = :status) as averageRating,
               (SELECT COUNT(r) FROM ModelReview r WHERE r.modelId = m.id AND r.status = :status) as reviewCount
        FROM AIModel m
        LEFT JOIN ModelStatistics s ON s.model.id = m.id
        LEFT JOIN Member mem ON mem.id = m.ownerId
        WHERE m.updatedAt >= :fromDateTime
        ORDER BY m.updatedAt ASC
        """)
    org.springframework.data.domain.Page<ModelIndexProjection> findModelIndexesUpdatedAfter(
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("status") ReviewStatus status,
            org.springframework.data.domain.Pageable pageable);

    
    @Query("""
       select new com.example.nomodel.model.command.application.dto.response.AdminAIModelResponseDto(
            cast(am.id as string),
            am.id,
            am.modelName,
            am.modelMetadata.prompt,
            am.ownType,
            am.price,
            am.isPublic,
            m.username,
            ms.usageCount,
            ms.viewCount,
            am.createdAt
       )
       from AIModel am
       left join Member m on am.ownerId = m.id
       left join ModelStatistics ms on am.id = ms.model.id
       where am.ownType = 'ADMIN'
            and (:searchKeyword is null or am.modelName like concat('%', :searchKeyword,'%'))
       """)
    Page<List<AdminAIModelResponseDto>> getAdminAIModel(
            @Param(value="searchKeyword")String searchKeyword,
            Pageable pageable);
    
}