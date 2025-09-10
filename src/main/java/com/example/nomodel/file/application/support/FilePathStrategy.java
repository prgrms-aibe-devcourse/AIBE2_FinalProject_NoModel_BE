// com.example.nomodel.file.application.support.FilePathStrategy

package com.example.nomodel.file.application.support;

import com.example.nomodel.file.domain.model.FileType;
import com.example.nomodel.file.domain.model.RelationType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public final class FilePathStrategy {

    private FilePathStrategy() {}

    /**
     * originalNameOrContentType에는
     *  - 멀티파트 저장 시: original filename (예: "foo.png")
     *  - 바이트 저장 시: content-type (예: "image/png")
     * 가 들어온다.
     */
    public static String buildObjectName(RelationType relType,
                                         Long relId,
                                         FileType fileType,
                                         String originalNameOrContentType) {

        String ext = resolveExtension(originalNameOrContentType);        // ".png" 같은 확장자
        String uuid = UUID.randomUUID().toString();

        switch (relType) {
            case REMOVE_BG: {
                // removebg/yyyy-MM/uuid.png
                String ym = DateTimeFormatter.ofPattern("yyyy-MM").format(LocalDate.now());
                return "removebg/" + ym + "/" + uuid + ext;
            }
            case AD:
            default: {
                String y = DateTimeFormatter.ofPattern("yyyy").format(LocalDate.now());
                String m = DateTimeFormatter.ofPattern("MM").format(LocalDate.now());
                String rel = relType.name().toLowerCase(Locale.ROOT);
                String ft = fileType.name().toLowerCase(Locale.ROOT);
                return rel + "/" + relId + "/" + ft + "/" + y + "/" + m + "/" + uuid + ext;
            }
        }
    }

    /** filename 또는 content-type에서 확장자 추출 */
    private static String resolveExtension(String nameOrType) {
        if (nameOrType == null || nameOrType.isBlank()) return ".bin";

        // content-type 형태면("image/png") → ".png"
        int slash = nameOrType.indexOf('/');
        if (slash > 0 && !nameOrType.contains(".")) {
            String sub = nameOrType.substring(slash + 1).trim().toLowerCase(Locale.ROOT);
            // jpeg → jpg 등 간단 맵핑
            if (sub.equals("jpeg")) sub = "jpg";
            // svg+xml 같은 복합 타입 처리
            int plus = sub.indexOf('+');
            if (plus > 0) sub = sub.substring(0, plus);
            return "." + safeExt(sub);
        }

        // 파일명 형태면 마지막 '.' 기준
        int dot = nameOrType.lastIndexOf('.');
        if (dot >= 0 && dot < nameOrType.length() - 1) {
            String sub = nameOrType.substring(dot + 1).trim().toLowerCase(Locale.ROOT);
            return "." + safeExt(sub);
        }
        return ".bin";
    }

    private static String safeExt(String ext) {
        switch (ext) {
            case "png":
            case "jpg":
            case "jpeg":
            case "webp":
            case "gif":
            case "bmp":
            case "tiff":
            case "svg":
                return ext.equals("jpeg") ? "jpg" : ext;
            default:
                return "bin";
        }
    }
}
