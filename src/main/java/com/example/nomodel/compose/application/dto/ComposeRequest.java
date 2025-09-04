package com.example.nomodel.compose.application.dto;

import com.example.nomodel.generationjob.domain.model.GenerationMode;

public record ComposeRequest(
        Long fileId,        // 누끼 PNG fileId
        String prompt,      // 장면/인물 프롬프트
        GenerationMode mode // SCENE | SUBJECT_SCENE
) {}
