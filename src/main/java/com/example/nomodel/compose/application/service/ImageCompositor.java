package com.example.nomodel.compose.application.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageCompositor {

    @Value("${google.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_API_URL = 
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image-preview:generateContent";

    /**
     * Gemini API를 사용하여 두 이미지를 합성합니다.
     * @param dressImage 옷 이미지 (첫 번째 이미지)
     * @param modelImage 모델 이미지 (두 번째 이미지)
     * @param customPrompt 사용자 정의 프롬프트 (null이면 기본 프롬프트 사용)
     * @return 합성된 이미지의 바이트 배열
     * @throws Exception 합성 중 오류 발생 시
     */
    public byte[] composite(byte[] dressImage, byte[] modelImage, String customPrompt) throws Exception {
        try {
            log.info("Starting image composition with Gemini API");
            
            // 이미지를 Base64로 인코딩
            String dressBase64 = Base64.getEncoder().encodeToString(dressImage);
            String modelBase64 = Base64.getEncoder().encodeToString(modelImage);

            // 프롬프트 설정 (커스텀 프롬프트가 없으면 기본 프롬프트 사용)
            String prompt = customPrompt != null && !customPrompt.trim().isEmpty() 
                ? customPrompt 
                : "Create a professional e-commerce fashion photo. Take the dress from the first image and let the person from the second image wear it. Generate a realistic, full-body shot of the person wearing the dress, with proper lighting and shadows adjusted to match the environment. Make it look natural and professional for e-commerce use.";

            // 요청 바디 생성
            GeminiRequest geminiRequest = createGeminiRequest(dressBase64, modelBase64, prompt);
            
            // API 호출
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            HttpEntity<GeminiRequest> requestEntity = new HttpEntity<>(geminiRequest, headers);
            
            log.info("Sending request to Gemini API");
            ResponseEntity<String> response = restTemplate.postForEntity(
                GEMINI_API_URL, 
                requestEntity, 
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Received successful response from Gemini API");
                return extractImageFromResponse(response.getBody());
            } else {
                log.error("Gemini API returned error status: {}", response.getStatusCode());
                throw new RuntimeException("Gemini API error: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error during image composition", e);
            throw new Exception("Failed to composite images: " + e.getMessage(), e);
        }
    }

    /**
     * 기본 프롬프트로 이미지 합성
     */
    public byte[] composite(byte[] dressImage, byte[] modelImage) throws Exception {
        return composite(dressImage, modelImage, null);
    }

    private GeminiRequest createGeminiRequest(String dressBase64, String modelBase64, String prompt) {
        InlineData dressData = new InlineData("image/png", dressBase64);
        InlineData modelData = new InlineData("image/png", modelBase64);
        
        Part dressPart = new Part(dressData, null);
        Part modelPart = new Part(modelData, null);
        Part textPart = new Part(null, prompt);

        Content content = new Content(List.of(dressPart, modelPart, textPart));
        
        return new GeminiRequest(List.of(content));
    }

    private byte[] extractImageFromResponse(String responseBody) throws Exception {
        try {
            // JSON 응답 파싱
            GeminiResponse geminiResponse = objectMapper.readValue(responseBody, GeminiResponse.class);
            
            if (geminiResponse.candidates != null && !geminiResponse.candidates.isEmpty()) {
                Candidate candidate = geminiResponse.candidates.get(0);
                if (candidate.content != null && candidate.content.parts != null && !candidate.content.parts.isEmpty()) {
                    for (Part part : candidate.content.parts) {
                        if (part.inlineData != null && part.inlineData.data != null) {
                            log.info("Successfully extracted image data from response");
                            return Base64.getDecoder().decode(part.inlineData.data);
                        }
                    }
                }
            }
            
            log.error("No image data found in response");
            throw new RuntimeException("No image data found in Gemini response");
            
        } catch (Exception e) {
            log.error("Failed to extract image from response", e);
            throw new Exception("Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }

    // DTO 클래스들
    public record GeminiRequest(List<Content> contents) {}

    public record Content(List<Part> parts) {}

    public record Part(
        @JsonProperty("inline_data") InlineData inlineData,
        String text
    ) {}

    public record InlineData(
        @JsonProperty("mime_type") String mimeType,
        String data
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeminiResponse(
        List<Candidate> candidates,
        PromptFeedback promptFeedback
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(
        Content content,
        String finishReason,
        Integer index,
        List<SafetyRating> safetyRatings
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PromptFeedback(
        List<SafetyRating> safetyRatings
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SafetyRating(
        String category,
        String probability
    ) {}
}