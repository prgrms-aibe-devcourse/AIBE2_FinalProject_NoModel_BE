package com.example.nomodel.compose.application.service;

public interface ImageCompositor {
    /**
     * @param scene   장면(배경) 이미지
     * @param cutout  누끼 PNG(알파 포함)
     * @return        합성 결과 (PNG 권장)
     */
    byte[] composite(byte[] scene, byte[] cutout) throws Exception;
}
