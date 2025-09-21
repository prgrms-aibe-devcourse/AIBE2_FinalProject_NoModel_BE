package com.example.nomodel.file.domain.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImgService {
    
    /**
     * 이미지 파일을 업로드하고 URL을 반환합니다.
     * 
     * @param file 업로드할 이미지 파일
     * @param fileName 저장할 파일명
     * @return 업로드된 이미지의 공개 URL
     */
    String uploadImage(MultipartFile file, String fileName);
    
    /**
     * 이미지 파일을 삭제합니다.
     * 
     * @param fileName 삭제할 파일명
     */
    void deleteImage(String fileName);
    
    /**
     * 이미지의 공개 URL을 생성합니다.
     * 
     * @param fileName 파일명
     * @return 이미지의 공개 URL
     */
    String getImageUrl(String fileName);

    // ▼ 신규: 바이트 업로드/다운로드 지원
    String uploadBytes(byte[] data, String contentType, String fileName);
    byte[] download(String fileName);
}