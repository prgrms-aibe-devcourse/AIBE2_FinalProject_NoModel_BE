package com.example.nomodel.file.domain.repository;

import com.example.nomodel.file.domain.model.File;
import com.example.nomodel.file.domain.model.FileType;
import com.example.nomodel.file.domain.model.RelationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FileJpaRepository extends JpaRepository<File, Long> {

    /**
     * 특정 관계의 파일 목록 조회
     */
    List<File> findByRelationTypeAndRelationId(RelationType relationType, Long relationId);

    /**
     * 특정 관계의 특정 타입 파일 목록 조회
     */
    List<File> findByRelationTypeAndRelationIdAndFileType(RelationType relationType, Long relationId, FileType fileType);

    /**
     * 특정 관계의 썸네일 파일 조회
     */
    @Query("SELECT f FROM File f WHERE f.relationType = :relationType AND f.relationId = :relationId AND f.fileType = 'THUMBNAIL'")
    List<File> findThumbnailsByRelation(@Param("relationType") RelationType relationType, 
                                       @Param("relationId") Long relationId);

    /**
     * 특정 관계의 프리뷰 파일 조회
     */
    @Query("SELECT f FROM File f WHERE f.relationType = :relationType AND f.relationId = :relationId AND f.fileType = 'PREVIEW'")
    List<File> findPreviewsByRelation(@Param("relationType") RelationType relationType, 
                                     @Param("relationId") Long relationId);

    /**
     * 특정 관계의 첫 번째 썸네일 파일 조회
     */
    @Query("SELECT f FROM File f WHERE f.relationType = :relationType AND f.relationId = :relationId AND f.fileType = 'THUMBNAIL' ORDER BY f.createdAt ASC")
    Optional<File> findFirstThumbnailByRelation(@Param("relationType") RelationType relationType, 
                                               @Param("relationId") Long relationId);

    /**
     * 파일 타입별 조회
     */
    List<File> findByFileType(FileType fileType);

    /**
     * 관계 타입별 조회
     */
    List<File> findByRelationType(RelationType relationType);

    /**
     * 특정 관계의 파일 수 카운트
     */
    long countByRelationTypeAndRelationId(RelationType relationType, Long relationId);

    /**
     * 특정 관계의 특정 타입 파일 수 카운트
     */
    long countByRelationTypeAndRelationIdAndFileType(RelationType relationType, Long relationId, FileType fileType);

    /**
     * 이미지 파일만 조회 (contentType이 'image/'로 시작)
     */
    @Query("SELECT f FROM File f WHERE f.contentType LIKE 'image/%'")
    List<File> findImageFiles();

    /**
     * 특정 관계의 이미지 파일 조회
     */
    @Query("SELECT f FROM File f WHERE f.relationType = :relationType AND f.relationId = :relationId AND f.contentType LIKE 'image/%'")
    List<File> findImageFilesByRelation(@Param("relationType") RelationType relationType, 
                                       @Param("relationId") Long relationId);

    /**
     * 파일 URL로 파일 조회
     */
    Optional<File> findByFileUrl(String fileUrl);

    /**
     * 파일명으로 파일 검색 (부분 일치)
     */
    List<File> findByFileNameContaining(String fileName);

    /**
     * 특정 관계에 속하는 모든 파일 삭제용 조회
     */
    @Query("SELECT f FROM File f WHERE f.relationType = :relationType AND f.relationId = :relationId")
    List<File> findAllByRelation(@Param("relationType") RelationType relationType, 
                                @Param("relationId") Long relationId);

    /**
     * 파일 존재 여부 확인
     */
    boolean existsByRelationTypeAndRelationIdAndFileType(RelationType relationType, Long relationId, FileType fileType);

    /**
     * ContentType별 파일 조회
     */
    List<File> findByContentType(String contentType);

    /**
     * 여러 모델의 썸네일 파일을 일괄 조회 (N+1 쿼리 방지)
     */
    @Query("SELECT f FROM File f WHERE f.relationType = 'MODEL' AND f.relationId IN :modelIds AND f.fileType = 'THUMBNAIL' ORDER BY f.relationId, f.createdAt ASC")
    List<File> findThumbnailFilesByModelIds(@Param("modelIds") List<Long> modelIds);

    /**
     * 여러 모델의 이미지 파일을 일괄 조회 (N+1 쿼리 방지)
     */
    @Query("SELECT f FROM File f WHERE f.relationType = 'MODEL' AND f.relationId IN :modelIds AND f.contentType LIKE 'image/%' ORDER BY f.relationId, f.createdAt ASC")
    List<File> findImageFilesByModelIds(@Param("modelIds") List<Long> modelIds);
    
    /**
     * 여러 모델의 대표 이미지를 일괄 조회 (N+1 쿼리 방지)
     */
    @Query("SELECT f FROM File f WHERE f.relationType = 'MODEL' AND f.relationId IN :modelIds AND f.isPrimary = true")
    List<File> findPrimaryImagesByModelIds(@Param("modelIds") List<Long> modelIds);
    
    /**
     * 특정 관계의 대표 이미지 조회
     */
    @Query("SELECT f FROM File f WHERE f.relationType = :relationType AND f.relationId = :relationId AND f.isPrimary = true")
    Optional<File> findPrimaryByRelation(@Param("relationType") RelationType relationType, 
                                         @Param("relationId") Long relationId);
    
    /**
     * 특정 관계의 현재 대표 이미지들 모두 해제
     */
    @Query("UPDATE File f SET f.isPrimary = false WHERE f.relationType = :relationType AND f.relationId = :relationId AND f.isPrimary = true")
    @Modifying
    void unsetAllPrimaryByRelation(@Param("relationType") RelationType relationType, 
                                   @Param("relationId") Long relationId);
}