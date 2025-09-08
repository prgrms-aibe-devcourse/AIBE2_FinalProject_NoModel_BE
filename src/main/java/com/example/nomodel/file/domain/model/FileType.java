package com.example.nomodel.file.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileType {

    ORIGINAL("원본", "ORIGINAL");

    private final String description;
    private final String value;
}