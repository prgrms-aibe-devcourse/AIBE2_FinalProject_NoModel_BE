package com.example.nomodel.generate.application.service.provider;

import com.example.nomodel.generationjob.domain.model.GenerationMode;
import java.util.Map;

public interface ImageGenerator {
    /**
     * @param mode   SCENE | SUBJECT | SUBJECT_SCENE
     * @param prompt 전체 프롬프트(장면/인물/의상/분위기 등 통합)
     * @param opts   model, size, seed, control 옵션 등
     * @return       생성 결과 이미지 바이트 (PNG/JPEG)
     */
    byte[] generate(GenerationMode mode, String prompt, Map<String, Object> opts) throws Exception;
}
