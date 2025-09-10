package com.example.nomodel.removebg.application.service.provider;

import com.example.nomodel.file.application.service.FileService;
import com.example.nomodel.file.domain.model.FileType;
import com.example.nomodel.file.domain.model.RelationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "BG_PROVIDER", havingValue = "removebg")
@RequiredArgsConstructor
public class RemoveBgBackgroundRemovalService implements BackgroundRemovalService {

    // (선택) 동일 타입 WebClient가 여러 개인 프로젝트에서 보다 명확하게 주입
    private final @Qualifier("removeBgWebClient") WebClient removeBgWebClient;

    private final FileService fileService;

    @Override
    public Long removeBackground(Long originalFileId, Map<String, Object> opts) throws Exception {
        // 1) 원본 로드
        byte[] original = fileService.loadAsBytes(originalFileId);

        // 2) form-data 구성
        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("image_file", original)
                .filename("input.png")
                .contentType(MediaType.APPLICATION_OCTET_STREAM);

        // 옵션: size/format (bg_color는 기본 투명이라 전송하지 않음)
        String size = String.valueOf(opts.getOrDefault("size", "auto"));     // auto | preview | small | medium | hd | 4k (요금제에 따라 상이)
        String format = String.valueOf(opts.getOrDefault("format", "png"));  // 투명 유지 위해 png 권장

        mb.part("size", size);
        mb.part("format", format);

        int retry = Integer.parseInt(String.valueOf(opts.getOrDefault("retry", 1)));
        long timeoutSec = Long.parseLong(String.valueOf(opts.getOrDefault("timeoutSec", 60)));

        log.info("[remove.bg] call start: fileId={}, size={}, format={}", originalFileId, size, format);

        // 3) 호출
        byte[] out = removeBgWebClient.post()
                .uri(uriBuilder -> uriBuilder.path("/v1.0/removebg").build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(mb.build())
                .retrieve()
                .onStatus(s -> !s.is2xxSuccessful(), r -> r.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new RuntimeException("remove.bg error: " + body))))
                .bodyToMono(byte[].class)
                .timeout(Duration.ofSeconds(timeoutSec))
                .retry(retry)
                .block();

        if (out == null || out.length == 0) {
            throw new RuntimeException("remove.bg empty response");
        }

        // 4) 저장
        Long resultId = fileService.saveBytes(out, "image/png", RelationType.REMOVE_BG, originalFileId, FileType.PREVIEW);
        log.info("[remove.bg] succeed: originalFileId={} -> resultFileId={}", originalFileId, resultId);
        return resultId;
    }
}
