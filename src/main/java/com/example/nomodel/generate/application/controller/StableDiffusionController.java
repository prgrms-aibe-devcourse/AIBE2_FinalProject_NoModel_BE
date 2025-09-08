package com.example.nomodel.generate.application.controller;

import com.example.nomodel.generate.application.service.StableDiffusionImageGenerator;
import com.example.nomodel.generationjob.domain.model.GenerationMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/generate/stable-diffusion")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "GEN_PROVIDER", havingValue = "stable-diffusion")
public class StableDiffusionController {

    private final StableDiffusionImageGenerator stableDiffusionImageGenerator;

    /**
     * 간단한 텍스트 프롬프트로 이미지 생성
     * 
     * GET /api/generate/stable-diffusion/generate?prompt=1girl,sweater,white background&mode=SUBJECT
     */
    @GetMapping("/generate")
    public ResponseEntity<byte[]> generateImage(
            @RequestParam String prompt,
            @RequestParam(defaultValue = "SCENE") GenerationMode mode,
            @RequestParam(defaultValue = "512") int width,
            @RequestParam(defaultValue = "512") int height,
            @RequestParam(defaultValue = "25") int steps,
            @RequestParam(defaultValue = "7.0") double cfgScale,
            @RequestParam(defaultValue = "(worst quality:2),(low quality:2),(normal quality:2),lowres,watermark") 
            String negativePrompt) {
        
        try {
            log.info("GET: Stable Diffusion image generation");
            log.info("Prompt: {}, Mode: {}, Size: {}x{}", prompt, mode, width, height);

            Map<String, Object> options = Map.of(
                "width", width,
                "height", height,
                "steps", steps,
                "cfg_scale", cfgScale,
                "negative_prompt", negativePrompt
            );

            byte[] imageBytes = stableDiffusionImageGenerator.generate(mode, prompt, options);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"generated_" + System.currentTimeMillis() + ".png\"")
                    .body(imageBytes);

        } catch (Exception e) {
            log.error("Error generating image: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                    .body(("Error: " + e.getMessage()).getBytes());
        }
    }

    /**
     * POST 방식으로 상세한 옵션과 함께 이미지 생성
     */
    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateImagePost(@RequestBody GenerateRequest request) {
        try {
            log.info("POST: Stable Diffusion image generation");
            log.info("Request: {}", request);

            Map<String, Object> options = Map.of(
                "width", request.getWidth(),
                "height", request.getHeight(),
                "steps", request.getSteps(),
                "cfg_scale", request.getCfgScale(),
                "negative_prompt", request.getNegativePrompt()
            );

            byte[] imageBytes = stableDiffusionImageGenerator.generate(request.getMode(), request.getPrompt(), options);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"generated_" + System.currentTimeMillis() + ".png\"")
                    .body(imageBytes);

        } catch (Exception e) {
            log.error("Error generating image: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                    .body(("Error: " + e.getMessage()).getBytes());
        }
    }

    /**
     * API 연결 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        try {
            // 간단한 테스트 이미지 생성으로 연결 확인
            Map<String, Object> options = Map.of(
                "width", 64,
                "height", 64,
                "steps", 1,
                "cfg_scale", 1.0,
                "negative_prompt", ""
            );

            stableDiffusionImageGenerator.generate(GenerationMode.SCENE, "test", options);
            return ResponseEntity.ok("✅ Stable Diffusion API connection OK");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("❌ Stable Diffusion API connection failed: " + e.getMessage());
        }
    }

    /**
     * 이미지 생성 요청 DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GenerateRequest {
        private String prompt;
        private GenerationMode mode = GenerationMode.SCENE;
        private int width = 512;
        private int height = 512;
        private int steps = 25;
        private double cfgScale = 7.0;
        private String negativePrompt = "(worst quality:2),(low quality:2),(normal quality:2),lowres,watermark";
    }
}