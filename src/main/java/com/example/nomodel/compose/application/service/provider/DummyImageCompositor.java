package com.example.nomodel.compose.application.service.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "COMPOSITOR_PROVIDER", havingValue = "dummy", matchIfMissing = true)
public class DummyImageCompositor implements ImageCompositor {
    @Override
    public byte[] composite(byte[] scene, byte[] cutout) {
        // 합성기는 다음 단계에서 실제로 구현.
        // 지금은 파이프라인만 확인하므로 scene 그대로 반환.
        return scene;
    }
}
