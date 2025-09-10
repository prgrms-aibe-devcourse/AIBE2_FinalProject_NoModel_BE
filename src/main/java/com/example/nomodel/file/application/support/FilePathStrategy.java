package com.example.nomodel.file.application.support;

import com.example.nomodel.file.domain.model.FileType;
import com.example.nomodel.file.domain.model.RelationType;

import java.time.LocalDate;
import java.util.UUID;

public final class FilePathStrategy {

    private FilePathStrategy() {}

    // Firebase에 저장할 "파일명" (= 객체 키) 생성
    public static String buildObjectName(RelationType relationType,
                                         Long relationId,
                                         FileType fileType,
                                         String extOrMime) {
        // 확장자 추출 (mime 또는 파일명에서)
        String ext = normalizeExt(extOrMime); // ".png", ".jpg" 등
        LocalDate d = LocalDate.now();
        String uid = UUID.randomUUID().toString();

        // 예: model/123/preview/2025/09/uid.png
        return String.format("%s/%d/%s/%04d/%02d/%s%s",
                relationType.name().toLowerCase(),
                relationId,
                fileType.name().toLowerCase(),
                d.getYear(), d.getMonthValue(),
                uid, ext);
    }

    private static String normalizeExt(String extOrMime) {
        if (extOrMime == null) return "";
        String v = extOrMime.toLowerCase();

        if (v.startsWith(".")) return v; // ".png"
        if (v.contains("/")) { // "image/png"
            String[] parts = v.split("/");
            return parts.length == 2 ? "." + parts[1] : "";
        }
        // "filename.png"
        int i = v.lastIndexOf('.');
        if (i >= 0) return v.substring(i);
        return "";
    }
}
