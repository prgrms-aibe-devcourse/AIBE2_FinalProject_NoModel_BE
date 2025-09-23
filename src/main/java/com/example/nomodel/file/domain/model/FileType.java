package com.example.nomodel.file.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileType {

    ORIGINAL("원본", "ORIGINAL"),
    PREVIEW("미리보기", "PREVIEW"),
    RESULT("결과", "RESULT");

    private final String description;
    @JsonValue
    private final String value;

    @JsonCreator
    public static FileType fromValue(String value) {
        if (value == null) return null;
        for (FileType t : values()) {
            if (t.value.equalsIgnoreCase(value)) return t;
        }
        throw new IllegalArgumentException("Unknown FileType: " + value);
    }
}
