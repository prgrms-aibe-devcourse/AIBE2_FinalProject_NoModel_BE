package com.example.nomodel.file.infrastructure.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.file.domain.service.ImgService;
import com.google.cloud.storage.*;
import com.google.firebase.cloud.StorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class FirebaseImgServiceImpl implements ImgService {

    @Value("${firebase.storage.bucket}")
    private String firebaseStorageBucket;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String[] ALLOWED_IMAGE_TYPES = {
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    };

    @Override
    public String uploadImage(MultipartFile file, String fileName) {
        validateFile(file);
        validateStorageBucket();

        try {
            Bucket bucket = StorageClient.getInstance().bucket(firebaseStorageBucket);
            Blob blob = bucket.create(fileName, file.getInputStream(), file.getContentType());
            return publicUrlFrom(blob);

        } catch (Exception e) {
            throw new ApplicationException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }


    @Override
    public void deleteImage(String fileName) {
        validateStorageBucket();

        try {
            Bucket bucket = StorageClient.getInstance().bucket(firebaseStorageBucket);
            Blob blob = bucket.get(fileName);

            if (blob == null || !blob.delete()) {
                throw new ApplicationException(ErrorCode.FILE_NOT_FOUND);
            }
        } catch (Exception e) {
            if (e instanceof ApplicationException) {
                throw e;
            }
            throw new ApplicationException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    @Override
    public String getImageUrl(String fileName) {
        validateStorageBucket();

        try {
            Bucket bucket = StorageClient.getInstance().bucket(firebaseStorageBucket);
            Blob blob = bucket.get(fileName);

            if (blob == null) {
                throw new ApplicationException(ErrorCode.FILE_NOT_FOUND);
            }

            return blob.getMediaLink();
        } catch (Exception e) {
            if (e instanceof ApplicationException) {
                throw e;
            }
            throw new ApplicationException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }

        // 파일 크기 검증
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApplicationException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        // 파일 타입 검증 (이미지만 허용)
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedImageType(contentType)) {
            throw new ApplicationException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    private boolean isAllowedImageType(String contentType) {
        for (String allowedType : ALLOWED_IMAGE_TYPES) {
            if (allowedType.equals(contentType)) {
                return true;
            }
        }
        return false;
    }

    private void validateStorageBucket() {
        if (firebaseStorageBucket == null || firebaseStorageBucket.trim().isEmpty()) {
            throw new ApplicationException(ErrorCode.FIREBASE_STORAGE_BUCKET_NOT_CONFIGURED);
        }
    }

    private String generateUniqueFileName(String originalFileName) {
        String uuid = UUID.randomUUID().toString();

        // 파일 확장자 추출
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            String extension = originalFileName.substring(lastDotIndex);
            String nameWithoutExtension = originalFileName.substring(0, lastDotIndex);
            return nameWithoutExtension + "_" + uuid + extension;
        }

        return originalFileName + "_" + uuid;
    }

    // 바이트 배열 업로드 (객체키 그대로 사용)
    @Override
    public String uploadBytes(byte[] data, String contentType, String fileName) {
        validateStorageBucket();
        try {
            Bucket bucket = StorageClient.getInstance().bucket(firebaseStorageBucket);
            Blob blob = bucket.create(fileName, data, contentType);
            return publicUrlFrom(blob);
        } catch (Exception e) {
            throw new ApplicationException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    // 객체키로 바이너리 다운로드
    @Override
    public byte[] download(String fileName) {
        validateStorageBucket();
        try {
            Bucket bucket = StorageClient.getInstance().bucket(firebaseStorageBucket);
            Blob blob = bucket.get(fileName);
            if (blob == null) throw new ApplicationException(ErrorCode.FILE_NOT_FOUND);
            return blob.getContent();
        } catch (Exception e) {
            if (e instanceof ApplicationException) throw e;
            throw new ApplicationException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    // 공개 URL 생성 유틸
    private String publicUrlFrom(Blob blob) {
        // Firebase Storage 공개 URL 패턴
        // https://firebasestorage.googleapis.com/v0/b/{bucket}/o/{objectName}?alt=media
        String encoded = java.net.URLEncoder.encode(blob.getName(), java.nio.charset.StandardCharsets.UTF_8);
        return "https://firebasestorage.googleapis.com/v0/b/" + blob.getBucket() + "/o/" + encoded + "?alt=media";
    }

}
