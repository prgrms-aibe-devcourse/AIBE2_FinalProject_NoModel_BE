package com.example.nomodel.model.application.dto.response.cache;

import java.util.StringJoiner;

/**
 * 모델 검색 캐시 키 생성 유틸리티
 * 검색 파라미터를 기반으로 고유한 캐시 키 생성
 */
public class ModelSearchCacheKey {

    /**
     * 검색 파라미터 기반 캐시 키 생성
     * null 값도 일관성 있게 처리
     */
    public static String generate(String keyword, Boolean isFree, int page, int size) {
        StringJoiner joiner = new StringJoiner("_");

        // keyword 처리
        if (keyword == null || keyword.trim().isEmpty()) {
            joiner.add("ALL");
        } else {
            // 대소문자 구분 없이, 공백 정규화
            joiner.add(keyword.trim().toLowerCase().replaceAll("\\s+", "-"));
        }

        // isFree 처리
        if (isFree == null) {
            joiner.add("ANY");
        } else {
            joiner.add(isFree ? "FREE" : "PAID");
        }

        // page와 size 추가
        joiner.add(String.valueOf(page));
        joiner.add(String.valueOf(size));

        return joiner.toString();
    }

    /**
     * 태그 기반 캐시 키 생성
     */
    public static String generateForTag(String tag, int page, int size) {
        StringJoiner joiner = new StringJoiner("_");
        joiner.add("TAG");
        joiner.add(tag.toLowerCase().replaceAll("\\s+", "-"));
        joiner.add(String.valueOf(page));
        joiner.add(String.valueOf(size));
        return joiner.toString();
    }

    /**
     * 복합 필터 기반 캐시 키 생성
     */
    public static String generateForFilters(String keyword, String tag, String minPrice, String maxPrice, int page, int size) {
        StringJoiner joiner = new StringJoiner("_");

        // keyword
        joiner.add(keyword == null || keyword.isEmpty() ? "ALL" : keyword.toLowerCase().replaceAll("\\s+", "-"));

        // tag
        joiner.add(tag == null || tag.isEmpty() ? "NOTAG" : tag.toLowerCase().replaceAll("\\s+", "-"));

        // price range
        joiner.add(minPrice == null ? "0" : minPrice);
        joiner.add(maxPrice == null ? "MAX" : maxPrice);

        // pagination
        joiner.add(String.valueOf(page));
        joiner.add(String.valueOf(size));

        return joiner.toString();
    }
}