package com.example.nomodel.generate.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Stable Diffusion API 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StableDiffusionResponse {
    
    /**
     * Base64 인코딩된 이미지 목록
     */
    private List<String> images;
    
    /**
     * 생성 파라미터 정보
     */
    private Object parameters;
    
    /**
     * 추가 정보
     */
    private String info;
}