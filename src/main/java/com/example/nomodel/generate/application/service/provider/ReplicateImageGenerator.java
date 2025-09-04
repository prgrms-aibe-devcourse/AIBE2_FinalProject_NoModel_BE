package com.example.nomodel.generate.application.service.provider;

import com.example.nomodel.generationjob.domain.model.GenerationMode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "GEN_PROVIDER", havingValue = "replicate")
public class ReplicateImageGenerator implements ImageGenerator {

    private final WebClient replicateWebClient;

    @Value("${REPLICATE_MODEL_VERSION}")
    private String modelVersion;

    @Value("${GEN_TIMEOUT_SEC:120}")
    private long timeoutSec;

    @Value("${GEN_RETRY_MAX:1}")
    private int retryMax;

    @Override
    public byte[] generate(GenerationMode mode, String prompt, Map<String, Object> opts) throws Exception {
        // 1) prediction 생성
        var createReq = Map.of(
                "version", modelVersion,
                "input", Map.of(
                        "prompt", buildPrompt(mode, prompt),
                        // 모델별 옵션: size/aspect_ratio/seed/num_outputs 등
                        "aspect_ratio", opts.getOrDefault("aspect_ratio", "9:16"),
                        "output_format", "png"
                )
        );

        var created = replicateWebClient.post()
                .uri("/v1/predictions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createReq)
                .retrieve()
                .onStatus(s -> !s.is2xxSuccessful(), r -> r.bodyToMono(String.class)
                        .flatMap(b -> Mono.error(new RuntimeException("Replicate create error: " + b))))
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(timeoutSec))
                .retry(retryMax)
                .block();

        String id = (String)((Map<?,?>)created).get("id");

        // 2) 폴링
        Map<?,?> polled;
        while (true) {
            polled = replicateWebClient.get()
                    .uri("/v1/predictions/{id}", id)
                    .retrieve()
                    .onStatus(s -> !s.is2xxSuccessful(), r -> r.bodyToMono(String.class)
                            .flatMap(b -> Mono.error(new RuntimeException("Replicate get error: " + b))))
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(timeoutSec))
                    .retry(retryMax)
                    .block();

            String status = String.valueOf(polled.get("status"));
            if ("succeeded".equals(status)) break;
            if ("failed".equals(status) || "canceled".equals(status)) {
                throw new RuntimeException("Replicate prediction failed: " + status + " / " + polled);
            }
            Thread.sleep(1600);
        }

        // 3) 결과 URL -> 바이트 다운로드
        var output = (List<?>) polled.get("output");
        if (output == null || output.isEmpty()) {
            throw new RuntimeException("Replicate empty output");
        }
        String url = String.valueOf(output.get(0));
        // 이미지 다운로드
        return replicateWebClient.get()
                .uri(URI.create(url))
                .accept(MediaType.ALL)
                .retrieve()
                .bodyToMono(ByteArrayResource.class)
                .map(ByteArrayResource::getByteArray)
                .timeout(Duration.ofSeconds(timeoutSec))
                .retry(retryMax)
                .block();
    }

    /** 모드에 맞춰 프롬프트를 살짝 보정 */
    private String buildPrompt(GenerationMode mode, String userPrompt) {
        String base = (userPrompt == null ? "" : userPrompt);
        if (mode == null) mode = GenerationMode.SCENE;

        return switch (mode) {
            case SCENE -> base + ", no text, high detail background, product area reserved";
            case SUBJECT_SCENE -> base + ", human model included, natural lighting, studio-quality, no text";
            default -> base + ", no text"; // 누락된 모드 대비 기본 처리
        };
    }
}
