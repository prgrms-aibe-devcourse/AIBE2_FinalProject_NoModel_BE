package com.example.nomodel.generate.application.service;

import com.example.nomodel.file.application.service.FileService;
import com.example.nomodel.file.domain.model.FileType;
import com.example.nomodel.file.domain.model.RelationType;
import com.example.nomodel.generationjob.domain.model.GenerationMode;
import com.example.nomodel.generate.application.dto.StableDiffusionRequest;
import com.example.nomodel.generate.application.dto.StableDiffusionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "GEN_PROVIDER", havingValue = "stable-diffusion")
public class StableDiffusionImageGenerator {

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
            .build();
    
    private final ObjectMapper objectMapper;
    private final FileService fileService;

    @Value("${STABLE_DIFFUSION_API_URL:http://220.127.239.150:7860}")
    private String apiUrl;

    @Value("${GEN_TIMEOUT_SEC:120}")
    private long timeoutSec;

    @Value("${GEN_RETRY_MAX:1}")
    private int retryMax;

    /**
     * Stable Diffusion API를 사용하여 이미지 생성 후 Firebase에 저장
     * @param mode 생성 모드
     * @param prompt 프롬프트
     * @param opts 추가 옵션
     * @return 저장된 파일 ID
     */
    public Long generate(GenerationMode mode, String prompt, Map<String, Object> opts) throws Exception {
        log.info("Generating image with Stable Diffusion API");
        log.info("Prompt: {}", prompt);
        log.info("Mode: {}", mode);

        try {
            // API 요청 데이터 구성
            StableDiffusionRequest request = buildRequest(mode, prompt, opts);
            
            log.info("Sending request to Stable Diffusion API: {}/sdapi/v1/txt2img", apiUrl);

            // WebClient를 사용하여 API 호출
            StableDiffusionResponse response = webClient.post()
                    .uri(apiUrl + "/sdapi/v1/txt2img")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), clientResponse -> 
                        clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                log.error("Stable Diffusion API error: {}", errorBody);
                                return Mono.error(new RuntimeException("Stable Diffusion API error: " + errorBody));
                            }))
                    .bodyToMono(StableDiffusionResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSec))
                    .retry(retryMax)
                    .block();

            // 응답 검증 및 이미지 데이터 추출
            if (response == null || response.getImages() == null || response.getImages().isEmpty()) {
                throw new RuntimeException("No images returned from Stable Diffusion API");
            }

            // Base64 디코딩하여 바이트 배열 변환
            String base64Image = response.getImages().get(0);
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            
            log.info("Image generation completed. Size: {} bytes", imageBytes.length);
            
            // Firebase에 저장
            // RelationType은 GENERATE 타입이 없으므로 MODEL 또는 새로운 타입을 추가해야 함
            // 여기서는 일단 MODEL로 설정하거나, opts에서 relationId를 받아서 처리
            Long relationId = getRelationId(opts);
            RelationType relationType = getRelationType(opts);
            
            Long fileId = fileService.saveBytes(
                imageBytes, 
                "image/png", 
                relationType, 
                relationId, 
                FileType.PREVIEW
            );
            
            log.info("✅ Image generation and save completed successfully. FileId: {}", fileId);
            return fileId;

        } catch (WebClientResponseException e) {
            log.error("HTTP error during Stable Diffusion API call: {}", e.getMessage());
            throw new RuntimeException("Stable Diffusion API HTTP error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error during image generation: {}", e.getMessage(), e);
            throw new RuntimeException("Image generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * GenerationMode와 옵션을 기반으로 Stable Diffusion API 요청 객체 생성
     */
    private StableDiffusionRequest buildRequest(GenerationMode mode, String prompt, Map<String, Object> opts) {
        // 기본값 설정
        int width = (Integer) opts.getOrDefault("width", 512);
        int height = (Integer) opts.getOrDefault("height", 512);
        int steps = (Integer) opts.getOrDefault("steps", 25);
        double cfgScale = ((Number) opts.getOrDefault("cfg_scale", 7.0)).doubleValue();
        String negativePrompt = (String) opts.getOrDefault("negative_prompt", 
            "(worst quality:2),(low quality:2),(normal quality:2),lowres,watermark");

        // 모드에 따른 프롬프트 보정
        String enhancedPrompt = buildPrompt(mode, prompt);

        return StableDiffusionRequest.builder()
                .prompt(enhancedPrompt)
                .negativePrompt(negativePrompt)
                .width(width)
                .height(height)
                .steps(steps)
                .cfgScale(cfgScale)
                .samplerIndex("DPM++ 2M Karras")
                .restoreFaces(false)
                .tiling(false)
                .nIter(1)
                .batchSize(1)
                .seed(-1)
                .subseed(-1)
                .subseedStrength(0)
                .seedResizeFromH(-1)
                .seedResizeFromW(-1)
                .enableHr(false)
                .saveImages(false)
                .sendImages(true)
                .doNotSaveSamples(true)
                .doNotSaveGrid(true)
                .build();
    }

    /**
     * 모드에 맞춰 프롬프트를 보정
     */
    private String buildPrompt(GenerationMode mode, String userPrompt) {
        String base = (userPrompt == null ? "" : userPrompt);
        if (mode == null) mode = GenerationMode.SCENE;

        return switch (mode) {
            case SCENE -> base + ", high quality background, detailed environment, no text, professional lighting";
            case SUBJECT -> base + ", 1girl, portrait, high quality, detailed face, studio lighting, no text";
            case SUBJECT_SCENE -> base + ", 1girl, full body, detailed background, natural lighting, high quality, no text";
        };
    }

    /**
     * 옵션에서 relationId를 추출
     * 없으면 기본값 0L 반환 (임시)
     */
    private Long getRelationId(Map<String, Object> opts) {
        Object relationId = opts.get("relationId");
        if (relationId instanceof Number) {
            return ((Number) relationId).longValue();
        }
        // 기본값으로 0L 반환 (실제로는 적절한 ID나 null 처리 필요)
        return 0L;
    }

    /**
     * 옵션에서 RelationType을 추출
     * 없으면 기본값 MODEL 반환
     */
    private RelationType getRelationType(Map<String, Object> opts) {
        Object relationType = opts.get("relationType");
        if (relationType instanceof String) {
            try {
                return RelationType.valueOf((String) relationType);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid RelationType: {}, using MODEL as default", relationType);
            }
        }
        // 기본값으로 MODEL 반환 (또는 GENERATE 타입을 RelationType에 추가해야 함)
        return RelationType.MODEL;
    }
}