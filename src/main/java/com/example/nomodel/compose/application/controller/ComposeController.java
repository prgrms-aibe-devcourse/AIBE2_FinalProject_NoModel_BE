package com.example.nomodel.compose.application.controller;

import com.example.nomodel.compose.application.dto.ComposeRequest;
import com.example.nomodel.compose.application.dto.ComposeResponse;
import com.example.nomodel.compose.application.service.ImageCompositor;
import com.example.nomodel.file.application.service.FileService;
import com.example.nomodel.file.domain.model.FileType;
import com.example.nomodel.file.domain.model.RelationType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/compose")
@Slf4j
public class ComposeController {

    private final ImageCompositor imageCompositor;
    private final FileService fileService;

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
            @RequestHeader(name = "X-User-Id", required = false) Long userId) {
        
        try {
            log.info("Received composition request for product file ID: {} and model file ID: {}", 
                    request.productFileId(), request.modelFileId());

            // 작업 ID 생성
            String jobId = UUID.randomUUID().toString();
            
            // 비동기로 처리 시작
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("Starting async composition for job: {}", jobId);
                    
                    // FileService를 통해 이미지 바이트 가져오기
                    byte[] productImage = fileService.loadAsBytes(request.productFileId().longValue());
                    byte[] modelImage = fileService.loadAsBytes(request.modelFileId().longValue());
                    
                    log.info("Retrieved images from Firebase - Product: {} bytes, Model: {} bytes", 
                            productImage.length, modelImage.length);
                    
                    // Python 스크립트를 통한 이미지 합성 실행
                    byte[] compositeResult = imageCompositor.composite(productImage, modelImage, request.customPrompt());
                    
                    log.info("Image composition completed - Result: {} bytes", compositeResult.length);
                    
                    // 합성 결과를 Firebase에 저장
                    Long resultFileId = fileService.saveBytes(
                        compositeResult,
                        "image/png",
                        RelationType.MODEL, // 적절한 RelationType 사용
                        0L, // 관련 ID (필요에 따라 수정)
                        FileType.RESULT
                    );
                    
                    // 결과 파일의 URL 가져오기
                    String resultFileUrl = fileService.getMeta(resultFileId).getFileUrl();
                    
                    log.info("Composition completed for job: {}, resultFileId: {}, url: {}", 
                            jobId, resultFileId, resultFileUrl);
                    
                    // TODO: 실제로는 Job 상태 관리 시스템에 결과를 저장해야 함
                    // jobService.updateJobResult(jobId, resultFileId.intValue(), resultFileUrl);
                    
                } catch (Exception e) {
                    log.error("Error during async composition for job: {}", jobId, e);
                    // TODO: Job 상태를 FAILED로 업데이트
                    // jobService.updateJobStatus(jobId, "FAILED", e.getMessage());
                }
            });

            // 처리 중 응답 즉시 반환
            return ResponseEntity.accepted()
                    .body(ComposeResponse.processing(jobId));

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
            @RequestHeader(name = "X-User-Id", required = false) Long userId) {

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
                RelationType.MODEL,
                0L,
                FileType.RESULT
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
            
            // TODO: 실제 구현에서는 작업 관리 시스템에서 상태를 조회
            // ComposeResponse response = jobService.getJobStatus(jobId);
            // return ResponseEntity.ok(response);
            
            // 예시 응답 (실제로는 Job 관리 시스템에서 가져와야 함)
            return ResponseEntity.ok(ComposeResponse.processing(jobId));
            
        } catch (Exception e) {
            log.error("Error checking job status for: {}", jobId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ComposeResponse.failure(jobId, "Failed to check job status"));
        }
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
