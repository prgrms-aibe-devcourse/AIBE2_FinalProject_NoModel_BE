package com.example.nomodel.removebg.application.service.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.example.nomodel.file.application.service.FileService;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "BG_PROVIDER", havingValue = "gemini")
@RequiredArgsConstructor
public class GeminiBackgroundRemovalService implements BackgroundRemovalService {

    private final WebClient geminiWebClient;
    private final FileService fileService;

    @Value("${GEMINI_BG_MODEL:gemini-2.0-pro}")
    private String model;

    @Value("${BG_TIMEOUT_SEC:120}")
    private long timeoutSec;

    @Override
    public Long removeBackground(Long originalFileId, Map<String,Object> opts) throws Exception {
        // 1) 원본 파일 읽기
        var original = fileService.loadAsBytes(originalFileId); // 바이트/메타 얻는 메서드는 FileService에 구현

        // 2) Gemini 호출 (엔드포인트/바디는 팀이 실제 스펙에 맞춰 조정)
        // 예시: /v1beta/models/{model}:media.edit (가상의 엔드포인트 이름, 실제로는 팀에서 사용 중인 게이트웨이/프록시와 맞추세요)
        var respBytes = geminiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/{model}:media:removeBackground")
                        .build(model))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(original)
                .retrieve()
                .onStatus(HttpStatusCode::isError, r -> r.bodyToMono(String.class).flatMap(b ->
                        Mono.error(new RuntimeException("Gemini error: " + b))))
                .bodyToMono(byte[].class)
                .timeout(Duration.ofSeconds(timeoutSec))
                .retry((int) Integer.parseInt(String.valueOf(opts.getOrDefault("retry", 2))))
                .block();

        if (respBytes == null) throw new RuntimeException("Empty response from Gemini");

        // 3) 결과 저장(type = REMOVED_BG) 후 id 반환
        return fileService.saveBytes(
                respBytes,
                "image/png",
                com.example.nomodel.file.domain.model.RelationType.AD,   // 임시: 광고 자산으로 묶기 (TODO: 필요하면 새 enum 추가)
                originalFileId,
                com.example.nomodel.file.domain.model.FileType.PREVIEW   // 임시: 제거본을 PREVIEW 로 저장
        );
    }
}
