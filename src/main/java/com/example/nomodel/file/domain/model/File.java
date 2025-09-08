package com.example.nomodel.file.domain.model;

import com.example.nomodel._core.common.BaseTimeEntity;
import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "file_tb")
public class File extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false)
    private RelationType relationType;

    @Column(name = "relation_id", nullable = false)
    private Long relationId;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;

    @Column(name = "content_type", length = 100)
    private String contentType; // MIME 타입

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false; // 대표 이미지 여부

    @Builder
    private File(RelationType relationType, Long relationId, String fileUrl, 
                String fileName, FileType fileType, String contentType, Boolean isPrimary) {
        this.relationType = relationType;
        this.relationId = relationId;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileType = fileType;
        this.contentType = contentType;
        this.isPrimary = isPrimary != null ? isPrimary : false;
    }

    public static File createFile(RelationType relationType, Long relationId, 
                                 String fileUrl, String fileName, FileType fileType) {
        return File.builder()
                .relationType(relationType)
                .relationId(relationId)
                .fileUrl(fileUrl)
                .fileName(fileName)
                .fileType(fileType)
                .build();
    }

    public static File createFileWithContentType(RelationType relationType, Long relationId,
                                                String fileUrl, String fileName, FileType fileType,
                                                String contentType) {
        return File.builder()
                .relationType(relationType)
                .relationId(relationId)
                .fileUrl(fileUrl)
                .fileName(fileName)
                .fileType(fileType)
                .contentType(contentType)
                .build();
    }

    public void updateUrl(String newFileUrl) {
        this.fileUrl = newFileUrl;
    }

    public void updateFileName(String newFileName) {
        this.fileName = newFileName;
    }

    /**
     * 특정 관계에 속하는 파일인지 확인
     * @param relationType 관계 타입
     * @param relationId 관계 ID
     * @return 해당 관계 파일 여부
     */
    public boolean belongsTo(RelationType relationType, Long relationId) {
        return this.relationType == relationType && this.relationId.equals(relationId);
    }

    /**
     * 이미지 파일인지 확인
     * @return 이미지 파일 여부
     */
    public boolean isImageFile() {
        if (this.contentType == null) {
            return false;
        }
        return this.contentType.startsWith("image/");
    }
    
    /**
     * 대표 이미지로 설정
     */
    public void setPrimary() {
        this.isPrimary = true;
    }
    
    /**
     * 대표 이미지 설정 해제
     */
    public void unsetPrimary() {
        this.isPrimary = false;
    }
    
    /**
     * 대표 이미지 상태 변경
     * @param isPrimary 대표 이미지 여부
     */
    public void updatePrimaryStatus(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
}