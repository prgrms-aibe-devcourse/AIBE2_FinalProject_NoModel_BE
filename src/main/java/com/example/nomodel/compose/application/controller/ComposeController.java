package com.example.nomodel.compose.application.controller;

import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel.compose.application.dto.ComposeRequest;
import com.example.nomodel.compose.application.dto.ComposeResponse;
import com.example.nomodel.compose.application.service.ImageCompositor;
import com.example.nomodel.file.application.service.FileService;
import com.example.nomodel.file.domain.model.FileType;
import com.example.nomodel.file.domain.model.RelationType;
import com.example.nomodel.generationjob.application.service.GenerationJobService;
import com.example.nomodel.generationjob.domain.model.GenerationJob;
import com.example.nomodel.generationjob.domain.model.GenerationMode;
import com.example.nomodel.generationjob.domain.model.JobStatus;
import com.example.nomodel.model.command.application.service.AdResultCommandService;
import com.example.nomodel.model.command.domain.model.AdResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/compose")
@Slf4j
public class ComposeController {

    private final ImageCompositor imageCompositor;
    private final FileService fileService;
    private final GenerationJobService generationJobService;
    private final AdResultCommandService adResultCommandService;

    /**
     * 파일 ID를 사용한 이미지 합성
     * 요청: POST /api/compose/compose
     * {
     *   "productFileId": 123,
     *   "modelFileId": 456,
     *   "customPrompt": "모델이 이 제품을 자연스럽게 광고하는 사진으로 바꿔줘"
     * }
     */
    @PostMapping("/compose")
    public ResponseEntity<ComposeResponse> composeWithFileIds(
            @Valid @RequestBody ComposeRequest request,
            @RequestHeader(name = "X-User-Id", required = false, defaultValue = "1") Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            log.info("Received composition request for product file ID: {} and model file ID: {}", 
                    request.productFileId(), request.modelFileId());

            // 동기 방식으로 즉시 처리
            GenerationJob job = GenerationJob.createComposeJob(userId, request.productFileId().longValue());
            job.setComposeParams(request.customPrompt(), GenerationMode.SUBJECT_SCENE);
            job.setModelId(request.modelFileId().longValue());
            job = generationJobService.getRepo().save(job);

            String jobId = job.getId().toString();
            
            // 입력 파일과 모델 파일의 URL 가져오기
            String inputFileUrl = fileService.getMeta(request.productFileId().longValue()).getFileUrl();
            String modelFileUrl = fileService.getMeta(request.modelFileId().longValue()).getFileUrl();
            
            try {
                // 동기 이미지 합성 실행
                job.markRunning();
                generationJobService.getRepo().save(job);
                
                // 제품 이미지와 모델 이미지 로드
                byte[] productImage = fileService.loadAsBytes(request.productFileId().longValue());
                byte[] modelImage = fileService.loadAsBytes(request.modelFileId().longValue());
                
                log.info("Retrieved images - Product: {} bytes, Model: {} bytes", 
                        productImage.length, modelImage.length);
                
                // ImageCompositor를 사용하여 이미지 합성
                byte[] compositeResult = imageCompositor.composite(productImage, modelImage, request.customPrompt());
                
                log.info("Image composition completed - Result: {} bytes", compositeResult.length);

                Long modelId = fileService.getModelId(request.modelFileId().longValue());
                AdResult adResult = adResultCommandService.createAdResult(modelId, userDetails.getMemberId(), request.customPrompt(), null); // TODO: ad_result 생성 후 ID 할당

                // 합성 결과를 Firebase에 저장 (임시로 PREVIEW 사용)
                Long resultFileId = fileService.saveBytes(
                    compositeResult,
                    "image/png",
                    RelationType.AD,
                    adResult.getId(), // ad_result_id를 relation_id로 사용
                    FileType.PREVIEW // RESULT 대신 PREVIEW 사용 (마이그레이션 적용 전까지 임시)
                );

                // 결과 파일 URL 가져오기
                String resultFileUrl = fileService.getMeta(resultFileId).getFileUrl();
                adResultCommandService.updateResultImageUrl(adResult, resultFileUrl);
                
                // Job 성공 처리
                job.succeed(resultFileId);
                generationJobService.getRepo().save(job);
                
                log.info("Composition completed for job: {}, resultFileId: {}", jobId, resultFileId);

                // 성공 응답 반환 (결과 포함)
                return ResponseEntity.ok()
                        .body(new ComposeResponse(
                            jobId,
                            "SUCCEEDED",
                            request.productFileId(),
                            request.modelFileId(),
                            resultFileId.intValue(),
                            resultFileUrl,
                            inputFileUrl,
                            modelFileUrl,
                            null, // errorMessage
                            LocalDateTime.now(),
                            LocalDateTime.now()
                        ));
                        
            } catch (Exception e) {
                log.error("Error during image composition for job: {}", jobId, e);
                
                // Job 실패 처리
                job.fail("Image composition failed: " + e.getMessage());
                generationJobService.getRepo().save(job);
                
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ComposeResponse.failure(jobId, "Image composition failed: " + e.getMessage()));
            }

        } catch (Exception e) {
            log.error("Error processing composition request", e);
            String jobId = UUID.randomUUID().toString();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ComposeResponse.failure(jobId, "Failed to process composition request: " + e.getMessage()));
        }
    }

    /**
     * 직접 파일 업로드를 통한 이미지 합성 (동기)
     * 테스트 및 개발용
     */
    @PostMapping(value = "/compose-direct", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ComposeResponse> composeWithDirectUpload(
            @RequestParam("productImage") MultipartFile productImage,
            @RequestParam("modelImage") MultipartFile modelImage,
            @RequestParam(value = "customPrompt", required = false) String customPrompt,
            @RequestHeader(name = "X-User-Id", required = false, defaultValue = "1") Long userId) {

        String jobId = UUID.randomUUID().toString();
        
        try {
            log.info("Received direct composition request with files: {} and {}", 
                    productImage.getOriginalFilename(), modelImage.getOriginalFilename());

            // 파일 유효성 검사
            if (productImage.isEmpty() || modelImage.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ComposeResponse.failure(jobId, "Both product and model images are required"));
            }

            // 이미지 파일 형식 검사
            if (!isImageFile(productImage) || !isImageFile(modelImage)) {
                return ResponseEntity.badRequest()
                        .body(ComposeResponse.failure(jobId, "Only image files are allowed"));
            }

            // 파일 크기 검사 (예: 10MB 제한)
            long maxSize = 10 * 1024 * 1024; // 10MB
            if (productImage.getSize() > maxSize || modelImage.getSize() > maxSize) {
                return ResponseEntity.badRequest()
                        .body(ComposeResponse.failure(jobId, "File size must be less than 10MB"));
            }

            // 이미지 합성 실행
            byte[] productBytes = productImage.getBytes();
            byte[] modelBytes = modelImage.getBytes();
            
            // Python 스크립트를 통한 이미지 합성 실행
            byte[] compositeResult = imageCompositor.composite(productBytes, modelBytes, customPrompt);

            // 합성 결과를 Firebase에 저장
            Long resultFileId = fileService.saveBytes(
                compositeResult,
                "image/png",
                RelationType.AD,
                0L,
                FileType.PREVIEW // RESULT 대신 PREVIEW 사용
            );
            
            // 결과 파일의 URL 가져오기
            String resultFileUrl = fileService.getMeta(resultFileId).getFileUrl();

            log.info("Composition completed successfully. Result size: {} bytes, fileId: {}", 
                    compositeResult.length, resultFileId);

            return ResponseEntity.ok(ComposeResponse.success(jobId, resultFileId.intValue(), resultFileUrl));

        } catch (IOException e) {
            log.error("Error reading uploaded files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ComposeResponse.failure(jobId, "Failed to read uploaded files"));
        } catch (Exception e) {
            log.error("Error during image composition", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ComposeResponse.failure(jobId, "Image composition failed: " + e.getMessage()));
        }
    }

    /**
     * 작업 상태 확인 엔드포인트
     */
    @GetMapping("/job/{jobId}/status")
    public ResponseEntity<ComposeResponse> getJobStatus(@PathVariable String jobId) {
        try {
            log.info("Checking status for job: {}", jobId);
            
            UUID jobUuid = UUID.fromString(jobId);
            GenerationJob job = generationJobService.view(jobUuid);
            
            // Job 상태에 따른 응답 생성
            return ResponseEntity.ok(createResponseFromJob(job));
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid job ID format: {}", jobId);
            return ResponseEntity.badRequest()
                    .body(ComposeResponse.failure(jobId, "Invalid job ID format"));
        } catch (Exception e) {
            log.error("Error checking job status for: {}", jobId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ComposeResponse.failure(jobId, "Failed to check job status"));
        }
    }

    /**
     * GenerationJob으로부터 ComposeResponse 생성
     */
    private ComposeResponse createResponseFromJob(GenerationJob job) {
        String inputFileUrl = null;
        String modelFileUrl = null;
        String resultFileUrl = null;
        
        try {
            // 입력 파일 URL 가져오기
            if (job.getInputFileId() != null) {
                inputFileUrl = fileService.getMeta(job.getInputFileId()).getFileUrl();
            }
            
            // 모델 파일 URL 가져오기
            if (job.getModelId() != null) {
                modelFileUrl = fileService.getMeta(job.getModelId()).getFileUrl();
            }
            
            // 결과 파일 URL 가져오기
            if (job.getResultFileId() != null) {
                resultFileUrl = fileService.getMeta(job.getResultFileId()).getFileUrl();
            }
        } catch (Exception e) {
            log.warn("Failed to get file URL for job {}: {}", job.getId(), e.getMessage());
        }
        
        String status = mapJobStatus(job.getStatus());
        
        return new ComposeResponse(
            job.getId().toString(),
            status,
            job.getInputFileId() != null ? job.getInputFileId().intValue() : null,
            job.getModelId() != null ? job.getModelId().intValue() : null,
            job.getResultFileId() != null ? job.getResultFileId().intValue() : null,
            resultFileUrl,
            inputFileUrl,
            modelFileUrl,
            job.getErrorMessage(),
            job.getCreatedAt(),
            job.getUpdatedAt()
        );
    }

    /**
     * JobStatus를 응답 상태로 매핑
     */
    private String mapJobStatus(JobStatus jobStatus) {
        return switch (jobStatus) {
            case PENDING, RUNNING -> "PROCESSING";
            case SUCCEEDED -> "SUCCEEDED";
            case FAILED -> "FAILED";
        };
    }

    /**
     * 이미지 파일 형식 검사
     */
    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (
                contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/gif") ||
                contentType.equals("image/webp")
        );
    }

    /**
     * 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ComposeResponse> handleException(Exception e) {
        log.error("Unhandled exception in ComposeController", e);
        String jobId = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ComposeResponse.failure(jobId, "An unexpected error occurred: " + e.getMessage()));
    }
}
