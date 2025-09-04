package com.example.nomodel.generate.application.service.provider;

import com.example.nomodel.generationjob.domain.model.GenerationMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;

/**
 * 실제 모델 호출 전 파이프라인 점검용 더미 생성기.
 * GEN_PROVIDER=dummy 이거나(권장) 생성기 Bean이 전혀 없을 때 자동 활성화.
 */
@Service
public class DummyImageGenerator implements ImageGenerator {

    // 1x1 transparent PNG
    private static final byte[] PNG_1x1_TRANSPARENT = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMBDQf2rCQAAAAASUVORK5CYII="
    );

    @Override
    public byte[] generate(GenerationMode mode, String prompt, Map<String, Object> opts) {
        return PNG_1x1_TRANSPARENT;
    }
}
