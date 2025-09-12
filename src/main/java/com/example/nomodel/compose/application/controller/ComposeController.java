package com.example.nomodel.compose.application.controller;

import com.example.nomodel.compose.application.dto.ComposeRequest;
import com.example.nomodel.compose.application.dto.ComposeResponse;
import com.example.nomodel.compose.application.service.ImageCompositor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/compose")
@Slf4j
public class ComposeController {

    private final ImageCompositor imageCompositor;

    /**
     * 파일 ID를 사용한 이미지 합성 (비동기)
     * 실제 구현에서는 파일 서비스에서 파일을 가져와야 합니다.
     */
    @PostMapping("/compose")
    public ResponseEntity<ComposeResponse> composeWithFileIds(
            @Valid @RequestBody ComposeRequest request,
            @RequestHeader(name = "X-User-Id", required = false) Long userId) {
        
        try {
            log.info("Received composition request for dress file ID: {} and model file ID: {}", 
                    request.dressFileId(), request.modelFileId());

            // TODO: 실제 구현에서는 FileService를 통해 파일 ID로부터 이미지 데이터를 가져와야 합니다.
            // 현재는 예시로 처리 중 응답을 반환합니다.
            
            // 비동기 처리를 위한 작업 ID 생성 (실제로는 큐나 작업 관리 시스템 사용)
            String jobId = "job_" + System.currentTimeMillis();
            
            // 비동기로 처리 시작
            CompletableFuture.runAsync(() -> {
                try {
                    // TODO: 여기서 실제 이미지 합성 로직 구현
                    log.info("Starting async composition for job: {}", jobId);
                    
                    // 예시: 파일 서비스에서 이미지 가져오기
                    // byte[] dressImage = fileService.getFileContent(request.dressFileId());
                    // byte[] modelImage = fileService.getFileContent(request.modelFileId());
                    
                    // 합성 실행
                    // byte[] result = imageCompositor.composite(dressImage, modelImage, request.customPrompt());
                    
                    // 결과 저장 및 알림
                    // fileService.saveCompositionResult(jobId, result);
                    
                    log.info("Composition completed for job: {}", jobId);
                } catch (Exception e) {
                    log.error("Error during async composition for job: {}", jobId, e);
                }
            });

            return ResponseEntity.accepted()
                    .body(ComposeResponse.processing(jobId));

        } catch (Exception e) {
            log.error("Error processing composition request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ComposeResponse.error("Failed to process composition request: " + e.getMessage()));
        }
    }

    /**
     * 직접 파일 업로드를 통한 이미지 합성 (동기)
     * 테스트 및 개발용
     */
    @PostMapping(value = "/compose-direct", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ComposeResponse> composeWithDirectUpload(
            @RequestParam("dressImage") MultipartFile dressImage,
            @RequestParam("modelImage") MultipartFile modelImage,
            @RequestParam(value = "customPrompt", required = false) String customPrompt,
            @RequestHeader(name = "X-User-Id", required = false) Long userId) {

        try {
            log.info("Received direct composition request with files: {} and {}", 
                    dressImage.getOriginalFilename(), modelImage.getOriginalFilename());

            // 파일 유효성 검사
            if (dressImage.isEmpty() || modelImage.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ComposeResponse.error("Both dress and model images are required"));
            }

            // 이미지 파일 형식 검사
            if (!isImageFile(dressImage) || !isImageFile(modelImage)) {
                return ResponseEntity.badRequest()
                        .body(ComposeResponse.error("Only image files are allowed"));
            }

            // 파일 크기 검사 (예: 10MB 제한)
            long maxSize = 10 * 1024 * 1024; // 10MB
            if (dressImage.getSize() > maxSize || modelImage.getSize() > maxSize) {
                return ResponseEntity.badRequest()
                        .body(ComposeResponse.error("File size must be less than 10MB"));
            }

            // 이미지 합성 실행
            byte[] dressBytes = dressImage.getBytes();
            byte[] modelBytes = modelImage.getBytes();
            
            byte[] compositeResult = imageCompositor.composite(dressBytes, modelBytes, customPrompt);

            // TODO: 실제 구현에서는 결과 이미지를 파일 서비스에 저장하고 URL/파일 ID를 반환
            // 현재는 예시로 성공 응답만 반환
            String resultUrl = "https://example.com/generated_image_" + System.currentTimeMillis() + ".png";
            Long resultFileId = System.currentTimeMillis();

            log.info("Composition completed successfully. Result size: {} bytes", compositeResult.length);

            return ResponseEntity.ok(ComposeResponse.success(resultUrl, resultFileId));

        } catch (IOException e) {
            log.error("Error reading uploaded files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ComposeResponse.error("Failed to read uploaded files", "FILE_READ_ERROR", e.getMessage()));
        } catch (Exception e) {
            log.error("Error during image composition", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ComposeResponse.error("Image composition failed", "COMPOSITION_ERROR", e.getMessage()));
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
            // JobStatus status = jobService.getJobStatus(jobId);
            
            // 예시 응답
            return ResponseEntity.ok(ComposeResponse.processing(jobId));
            
        } catch (Exception e) {
            log.error("Error checking job status for: {}", jobId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ComposeResponse.error("Failed to check job status"));
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
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ComposeResponse.error("An unexpected error occurred", "INTERNAL_ERROR", e.getMessage()));
    }
}