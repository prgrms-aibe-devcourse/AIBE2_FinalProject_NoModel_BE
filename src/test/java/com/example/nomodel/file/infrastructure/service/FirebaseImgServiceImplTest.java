package com.example.nomodel.file.infrastructure.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@DisplayName("FirebaseImgServiceImpl 테스트")
class FirebaseImgServiceImplTest {

    @Mock
    private FirebaseApp firebaseApp;

    @InjectMocks
    private FirebaseImgServiceImpl imgService;

    private MultipartFile validImageFile;
    private MultipartFile invalidFile;
    private MultipartFile oversizedFile;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(imgService, "firebaseStorageBucket", "test-bucket");

        // 유효한 이미지 파일 (1MB)
        byte[] imageContent = new byte[1024 * 1024];
        validImageFile = new MockMultipartFile(
                "image",
                "test-image.jpg",
                "image/jpeg",
                imageContent
        );

        // 잘못된 파일 타입
        invalidFile = new MockMultipartFile(
                "document",
                "test-document.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        // 크기가 큰 파일 (15MB)
        byte[] oversizedContent = new byte[15 * 1024 * 1024];
        oversizedFile = new MockMultipartFile(
                "image",
                "large-image.jpg",
                "image/jpeg",
                oversizedContent
        );
    }

    @Test
    @DisplayName("null 파일 업로드 시 예외 발생")
    void uploadImage_WithNullFile_ShouldThrowException() {
        // when & then
        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> imgService.uploadImage(null, "test.jpg")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("빈 파일 업로드 시 예외 발생")
    void uploadImage_WithEmptyFile_ShouldThrowException() {
        // given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "empty",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        // when & then
        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> imgService.uploadImage(emptyFile, "test.jpg")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("허용되지 않는 파일 타입 업로드 시 예외 발생")
    void uploadImage_WithInvalidFileType_ShouldThrowException() {
        // when & then
        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> imgService.uploadImage(invalidFile, "test.pdf")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_FILE_TYPE);
    }

    @Test
    @DisplayName("파일 크기 초과 시 예외 발생")
    void uploadImage_WithOversizedFile_ShouldThrowException() {
        // when & then
        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> imgService.uploadImage(oversizedFile, "large.jpg")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED);
    }

    @Test
    @DisplayName("Firebase Storage Bucket이 설정되지 않은 경우 예외 발생")
    void uploadImage_WithoutStorageBucket_ShouldThrowException() {
        // given
        ReflectionTestUtils.setField(imgService, "firebaseStorageBucket", "");

        // when & then
        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> imgService.uploadImage(validImageFile, "test.jpg")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FIREBASE_STORAGE_BUCKET_NOT_CONFIGURED);
    }

    @Test
    @DisplayName("파일 URL 생성")
    void getImageUrl_ShouldReturnValidUrl() {
        // given
        String fileName = "test-image.jpg";
        String expectedUrl = "https://storage.googleapis.com/test-bucket/test-image.jpg";

        // when
        String actualUrl = imgService.getImageUrl(fileName);

        // then
        assertThat(actualUrl).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("Storage Bucket이 설정되지 않은 경우 URL 생성 시 예외 발생")
    void getImageUrl_WithoutStorageBucket_ShouldThrowException() {
        // given
        ReflectionTestUtils.setField(imgService, "firebaseStorageBucket", null);

        // when & then
        ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> imgService.getImageUrl("test.jpg")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FIREBASE_STORAGE_BUCKET_NOT_CONFIGURED);
    }

    @Test
    @DisplayName("허용된 이미지 타입 검증")
    void validateAllowedImageTypes() {
        // given
        String[] allowedTypes = {"image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"};

        for (String contentType : allowedTypes) {
            MockMultipartFile testFile = new MockMultipartFile(
                    "image",
                    "test." + contentType.split("/")[1],
                    contentType,
                    new byte[1024]
            );

            // when & then - 예외가 발생하지 않아야 함
            assertThatNoException().isThrownBy(() -> {
                // private 메소드 테스트를 위해 실제 업로드는 하지 않고 validation만 확인
                try {
                    imgService.uploadImage(testFile, "test.jpg");
                } catch (ApplicationException e) {
                    // Firebase Storage 관련 에러가 아닌 다른 validation 에러인지 확인
                    if (e.getErrorCode() != ErrorCode.INVALID_FILE_TYPE) {
                        // 파일 타입은 유효하므로 다른 에러는 허용
                        return;
                    }
                    throw e;
                }
            });
        }
    }
}