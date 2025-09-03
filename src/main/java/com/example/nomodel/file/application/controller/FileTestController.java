package com.example.nomodel.file.application.controller;

import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.file.domain.service.ImgService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "File Test API", description = "파일 업로드 테스트 API")
@RestController
@RequestMapping("/api/test/files")
@RequiredArgsConstructor
public class FileTestController {

    private final ImgService imgService;

    @Operation(summary = "이미지 업로드 테스트", description = "Firebase Storage에 이미지를 업로드합니다.")
    @PostMapping("/upload")
    public ResponseEntity<ApiUtils.ApiResult<String>> uploadImage(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "파일명 (선택사항)")
            @RequestParam(value = "fileName", required = false) String fileName) {

        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = file.getOriginalFilename();
        }

        String imageUrl = imgService.uploadImage(file, fileName);
        
        return ResponseEntity.ok(ApiUtils.success(imageUrl));
    }

    @Operation(summary = "이미지 URL 조회", description = "파일명으로 이미지 URL을 조회합니다.")
    @GetMapping("/url")
    public ResponseEntity<ApiUtils.ApiResult<String>> getImageUrl(
            @Parameter(description = "파일명", required = true)
            @RequestParam("fileName") String fileName) {
        
        String imageUrl = imgService.getImageUrl(fileName);
        
        return ResponseEntity.ok(ApiUtils.success(imageUrl));
    }

    @Operation(summary = "이미지 삭제", description = "Firebase Storage에서 이미지를 삭제합니다.")
    @DeleteMapping("/delete")
    public ResponseEntity<ApiUtils.ApiResult<String>> deleteImage(
            @Parameter(description = "삭제할 파일명", required = true)
            @RequestParam("fileName") String fileName) {
        
        imgService.deleteImage(fileName);
        
        return ResponseEntity.ok(ApiUtils.success("Image deleted successfully"));
    }
}