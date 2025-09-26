package com.example.nomodel.file.application.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.file.application.support.FilePathStrategy;
import com.example.nomodel.file.domain.model.File;
import com.example.nomodel.file.domain.model.FileType;
import com.example.nomodel.file.domain.model.RelationType;
import com.example.nomodel.file.domain.repository.FileJpaRepository;
import com.example.nomodel.file.domain.service.ImgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileJpaRepository fileJpaRepository;
    private final ImgService imgService; // ← Firebase 어댑터

    /**
     * 여러 모델의 이미지 URL을 일괄 조회 (N+1 쿼리 방지)
     */
    public Map<Long, List<String>> getImageUrlsMap(List<Long> modelIds) {
        if (modelIds == null || modelIds.isEmpty()) {
            return Map.of();
        }

        List<File> imageFiles = fileJpaRepository.findImageFilesByModelIds(modelIds);

        return imageFiles.stream()
                .collect(Collectors.groupingBy(
                    File::getRelationId,
                    Collectors.mapping(File::getFileUrl, Collectors.toList())
                ));
    }

    @Transactional
    public Long saveFile(MultipartFile multipartFile,
                         RelationType relationType,
                         Long relationId,
                         FileType fileType) {

        String objectName = FilePathStrategy.buildObjectName(
                relationType, relationId, fileType, multipartFile.getOriginalFilename());

        // Firebase 업로드 → 공개 URL 반환
        String publicUrl = imgService.uploadImage(multipartFile, objectName);

        File fileEntity = File.createFileWithContentType(
                relationType,
                relationId,
                publicUrl,           // URL 저장
                objectName,          // fileName엔 "객체 키" 저장
                fileType,
                multipartFile.getContentType()
        );
        File saved = fileJpaRepository.save(fileEntity);
        return saved.getId();
    }

    @Transactional
    public Long saveBytes(byte[] data,
                          String contentType,
                          RelationType relationType,
                          Long relationId,
                          FileType fileType) {

        String objectName = FilePathStrategy.buildObjectName(
                relationType, relationId, fileType, contentType);

        String publicUrl = imgService.uploadBytes(data, contentType, objectName);

        File fileEntity = File.createFileWithContentType(
                relationType,
                relationId,
                publicUrl,
                objectName,
                fileType,
                contentType
        );
        File saved = fileJpaRepository.save(fileEntity);
        return saved.getId();
    }

    /**
     * 과거 레코드 호환을 위해:
     * 1) file_name(=객체 키)로 다운로드 시도
     * 2) 실패 시 file_url에서 객체명을 추출해 재시도
     * 3) 성공하면 file_name을 추출한 객체명으로 교정 저장(자동 복구)
     */
    @Transactional // ← 교정 저장을 위해 readOnly 제거
    public byte[] loadAsBytes(Long fileId) {
        File file = fileJpaRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        // 1) 최신 규약: file_name을 '객체 키'로 사용
        try {
            return imgService.download(file.getFileName());
        } catch (ApplicationException ignore) {
            // FALLBACK 진행
        }

        // 2) 과거 레코드 호환: file_url에서 객체명 추출 → 재시도
        String keyFromUrl = extractObjectNameFromUrl(file.getFileUrl());
        if (keyFromUrl != null && !keyFromUrl.isBlank()) {
            byte[] bytes = imgService.download(keyFromUrl);
            // 3) 성공하면 앞으로는 확실히 찾도록 자동 교정
            file.updateFileName(keyFromUrl);
            fileJpaRepository.save(file);
            return bytes;
        }

        throw new ApplicationException(ErrorCode.FILE_NOT_FOUND);
    }

    @Transactional(readOnly = true)
    public File getMeta(Long fileId) {
        return fileJpaRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));
    }

    /**
     * file_url 형식:
     *   https://firebasestorage.googleapis.com/v0/b/{bucket}/o/{URL-ENCODED_OBJECT_NAME}?alt=media
     * 또는
     *   https://storage.googleapis.com/download/storage/v1/b/{bucket}/o/{URL-ENCODED_OBJECT_NAME}?...
     * 에서 /o/와 ? 사이를 URL decode 해서 객체명을 얻는다.
     */
    private String extractObjectNameFromUrl(String url) {
        if (url == null) return null;
        int i = url.indexOf("/o/");
        if (i < 0) return null;
        int q = url.indexOf('?', i);
        String enc = (q > i) ? url.substring(i + 3, q) : url.substring(i + 3);
        try {
            return URLDecoder.decode(enc, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public Long getModelId(long modelFileId) {
        return fileJpaRepository.findById(modelFileId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.FILE_NOT_FOUND))
                .getRelationId();
    }
}
