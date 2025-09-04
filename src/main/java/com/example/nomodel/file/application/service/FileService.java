package com.example.nomodel.file.application.service;

import com.example.nomodel.file.domain.model.File;
import com.example.nomodel.file.domain.model.FileType;
import com.example.nomodel.file.domain.model.RelationType;
import com.example.nomodel.file.domain.repository.FileJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.example.nomodel.file.domain.model.File;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileJpaRepository fileJpaRepository;

    private final String BASE_PATH = "uploads"; // 로컬 저장 경로 (임시)

    /**
     * MultipartFile 저장
     */
    @Transactional
    public Long saveFile(MultipartFile multipartFile,
                         RelationType relationType,
                         Long relationId,
                         FileType fileType) throws IOException {

        // 1. 로컬 디렉토리 없으면 생성
        java.io.File dir = new java.io.File(BASE_PATH);
        if (!dir.exists()) dir.mkdirs();

        // 2. 저장 파일명 (UUID + 확장자)
        String originalName = multipartFile.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf("."))
                : "";
        String savedName = UUID.randomUUID() + ext;

        Path savePath = Paths.get(BASE_PATH, savedName);

        // 3. 파일 저장
        multipartFile.transferTo(savePath);

        // 4. DB 저장
        File fileEntity = File.createFileWithContentType(
                relationType,
                relationId,
                savePath.toString(),
                savedName,
                fileType,
                multipartFile.getContentType()
        );
        File saved = fileJpaRepository.save(fileEntity);

        return saved.getId();
    }

    /**
     * 바이트 배열 저장 (예: Gemini 결과물)
     */
    @Transactional
    public Long saveBytes(byte[] data,
                          String contentType,
                          RelationType relationType,
                          Long relationId,
                          FileType fileType) throws IOException {

        java.io.File dir = new java.io.File(BASE_PATH);
        if (!dir.exists()) dir.mkdirs();

        String savedName = UUID.randomUUID() + ".png";
        Path savePath = Paths.get(BASE_PATH, savedName);
        Files.write(savePath, data);

        File fileEntity = File.createFileWithContentType(
                relationType,
                relationId,
                savePath.toString(),
                savedName,
                fileType,
                contentType
        );
        File saved = fileJpaRepository.save(fileEntity);

        return saved.getId();
    }

    /**
     * 파일 로드
     */
    @Transactional(readOnly = true)
    public byte[] loadAsBytes(Long fileId) throws IOException {
        File file = fileJpaRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));
        Path path = Paths.get(file.getFileUrl());
        return Files.readAllBytes(path);
    }

    @Transactional(readOnly = true)
    public File getMeta(Long fileId) {
        return fileJpaRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));
    }
}

