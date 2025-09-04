package com.example.nomodel.generationjob.domain.model;

public enum GenerationMode {
    SCENE,          // 배경/장면만 생성
    SUBJECT,        // 인물(모델)만 생성
    SUBJECT_SCENE   // 인물 + 배경 한 번에 생성
}
