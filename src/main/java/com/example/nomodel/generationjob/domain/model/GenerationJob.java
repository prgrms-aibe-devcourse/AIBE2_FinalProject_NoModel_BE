package com.example.nomodel.generationjob.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

/**
 * 생성 잡(누끼/합성 등) 공용 엔티티
 */
@Entity
@Table(
        name = "generation_job",
        indexes = {
                @Index(name = "ix_job_owner", columnList = "ownerId"),
                @Index(name = "ix_job_status_createdAt", columnList = "status, createdAt")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 요청자(회원) */
    @Column(nullable = false)
    private Long ownerId;

    /** 잡 종류: REMOVE_BG, COMPOSE ... */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobType type;

    /** 입력 파일 (원본/누끼 등 파이프라인 입력) */
    @Column(nullable = false)
    private Long inputFileId;

    /** 합성 시 사용할 모델 아이디(선택) */
    private Long modelId;

    /** 파라미터 JSON(선택) */
    @Lob
    @Column(columnDefinition = "text")
    private String parameters;

    /** 잡 상태 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    /** 결과 파일 id */
    private Long resultFileId;

    /** 에러 메시지 */
    @Column(length = 1000)
    private String errorMessage;

    /** 재시도 횟수 */
    @Column(nullable = false)
    private int retryCount;

    /** 생성/수정 시각 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** 합성(배경/인물+배경) 프롬프트 */
    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    /** 합성 모드: SCENE | SUBJECT_SCENE ... */
    @Enumerated(EnumType.STRING)
    @Column(name = "mode", length = 20)
    private GenerationMode mode;

    /* ====================== 라이프사이클 ====================== */

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
        if (status == null) status = JobStatus.PENDING;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /* ====================== 상태 전이 헬퍼 ====================== */

    public void markRunning() {
        status = JobStatus.RUNNING;
    }

    public void succeed(Long resultFileId) {
        status = JobStatus.SUCCEEDED;
        this.resultFileId = resultFileId;
    }

    public void fail(String msg) {
        status = JobStatus.FAILED;
        this.errorMessage = msg;
    }

    /* ====================== 합성 파라미터 세팅 ====================== */

    /** compose enqueue 시 편의 메서드 */
    public void setComposeParams(String prompt, GenerationMode mode) {
        this.prompt = prompt;
        this.mode = mode;
    }

    /* ====================== 팩토리 메서드(편의) ====================== */

    /** 합성 잡 생성 */
    public static GenerationJob createComposeJob(Long ownerId, Long inputFileId) {
        return GenerationJob.builder()
                .ownerId(ownerId)
                .type(JobType.COMPOSE)
                .inputFileId(inputFileId)
                .status(JobStatus.PENDING)
                .retryCount(0)
                .build();
    }

    /** 누끼 잡 생성(필요 시 사용) */
    public static GenerationJob createRemoveBgJob(Long ownerId, Long inputFileId) {
        return GenerationJob.builder()
                .ownerId(ownerId)
                .type(JobType.REMOVE_BG)
                .inputFileId(inputFileId)
                .status(JobStatus.PENDING)
                .retryCount(0)
                .build();
    }
}
