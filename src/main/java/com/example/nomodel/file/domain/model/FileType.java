package com.example.nomodel.file.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileType {

    THUMBNAIL("썸네일", "THUMBNAIL"),
    PREVIEW("프리뷰", "PREVIEW");

    private final String description;
    private final String value;

    public boolean isThumbnail() {
        return this == THUMBNAIL;
    }

    public boolean isPreview() {
        return this == PREVIEW;
    }
}