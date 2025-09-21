package com.example.nomodel.file.application.controller;

import com.example.nomodel.file.application.service.FileService;
import com.example.nomodel.file.domain.model.FileType;
import com.example.nomodel.file.domain.model.RelationType;
import com.example.nomodel.file.domain.model.File;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;

    /** 업로드 */
    @PostMapping
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws Exception {
        Long fileId = fileService.saveFile(file, RelationType.AD, 0L, FileType.PREVIEW); // 임시 관계
        return ResponseEntity.ok().body(java.util.Map.of("fileId", fileId));
    }

    /** 파일 바이너리 조회 (브라우저에서 바로 보기) */
    @GetMapping("/{fileId}")
    public ResponseEntity<byte[]> view(@PathVariable Long fileId) throws Exception {
        File meta = fileService.getMeta(fileId);          // FileService에 getMeta(Long) 존재해야 함
        byte[] bytes = fileService.loadAsBytes(fileId);   // FileService에 loadAsBytes(Long) 존재해야 함

        String contentType = meta.getContentType() != null
                ? meta.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String fileName = meta.getFileName() != null ? meta.getFileName() : (fileId + "");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(bytes);
    }

    /** 파일 메타데이터만 JSON으로 조회 (디버깅/프론트 용) */
    @GetMapping("/{fileId}/meta")
    public ResponseEntity<?> meta(@PathVariable Long fileId) {
        File meta = fileService.getMeta(fileId);
        return ResponseEntity.ok(java.util.Map.of(
                "id", meta.getId(),
                "fileName", meta.getFileName(),
                "fileUrl", meta.getFileUrl(),
                "contentType", meta.getContentType(),
                "relationType", meta.getRelationType().name(),
                "relationId", meta.getRelationId(),
                "fileType", meta.getFileType().name(),
                "createdAt", meta.getCreatedAt(),
                "updatedAt", meta.getUpdatedAt()
        ));
    }
}
