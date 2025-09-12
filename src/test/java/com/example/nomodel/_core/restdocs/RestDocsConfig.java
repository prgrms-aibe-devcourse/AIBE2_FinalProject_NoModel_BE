package com.example.nomodel._core.restdocs;

import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.request.ParameterDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;

/**
 * REST Docs 공통 설정 및 재사용 가능한 필드/파라미터 정의
 */
public class RestDocsConfig {

    // ========== 공통 응답 구조 ==========
    
    /**
     * 성공 응답의 기본 필드 (success, response, error)
     */
    public static FieldDescriptor[] baseSuccessResponse() {
        return new FieldDescriptor[]{
            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
            fieldWithPath("response").type(JsonFieldType.OBJECT).description("응답 데이터"),
            fieldWithPath("error").type(JsonFieldType.NULL).description("에러 정보").optional()
        };
    }

    /**
     * 에러 응답의 기본 필드
     */
    public static FieldDescriptor[] baseErrorResponse() {
        return new FieldDescriptor[]{
            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
            fieldWithPath("response").type(JsonFieldType.NULL).description("응답 데이터").optional(),
            fieldWithPath("error").type(JsonFieldType.OBJECT).description("에러 정보"),
            fieldWithPath("error.status").type(JsonFieldType.NUMBER).description("HTTP 상태 코드")
        };
    }

    // ========== 페이징 관련 ==========
    
    /**
     * 기본 페이징 파라미터 (page, size)
     */
    public static ParameterDescriptor[] pagingParams() {
        return new ParameterDescriptor[]{
            parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
            parameterWithName("size").description("페이지 크기 (최대 100)").optional()
        };
    }

    /**
     * 페이징 응답 필드
     */
    public static FieldDescriptor[] pagingFields(String prefix) {
        return new FieldDescriptor[]{
            fieldWithPath(prefix + "pageNumber").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
            fieldWithPath(prefix + "pageSize").type(JsonFieldType.NUMBER).description("페이지 크기"),
            fieldWithPath(prefix + "totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
            fieldWithPath(prefix + "totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
            fieldWithPath(prefix + "hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부"),
            fieldWithPath(prefix + "hasPrevious").type(JsonFieldType.BOOLEAN).description("이전 페이지 존재 여부")
        };
    }

    // ========== 모델 사용 관련 ==========
    
    /**
     * 모델 사용 내역 쿼리 파라미터 (page, size, modelId)
     */
    public static ParameterDescriptor[] modelUsageHistoryParams() {
        return new ParameterDescriptor[]{
            parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
            parameterWithName("size").description("페이지 크기 (최대 100)").optional(),
            parameterWithName("modelId").description("특정 모델 ID 필터링").optional()
        };
    }

    /**
     * 모델 사용 내역 content 배열의 각 요소 필드
     */
    public static FieldDescriptor[] modelUsageHistoryContentFields() {
        return new FieldDescriptor[]{
            fieldWithPath("response.content[].adResultId").type(JsonFieldType.NUMBER).description("광고 결과 ID"),
            fieldWithPath("response.content[].modelId").type(JsonFieldType.NUMBER).description("모델 ID"),
            fieldWithPath("response.content[].modelName").type(JsonFieldType.STRING).description("모델명"),
            fieldWithPath("response.content[].prompt").type(JsonFieldType.STRING).description("입력 프롬프트"),
            fieldWithPath("response.content[].modelImageUrl").type(JsonFieldType.STRING).description("모델 이미지 URL").optional(),
            fieldWithPath("response.content[].createdAt").type(JsonFieldType.STRING).description("생성일시")
        };
    }

    /**
     * 모델 사용 내역 페이지 응답 전체 필드 (기본 응답 + content + 페이징)
     */
    public static FieldDescriptor[] modelUsageHistoryResponse() {
        List<FieldDescriptor> fields = new ArrayList<>();
        
        // 기본 응답 구조
        fields.addAll(Arrays.asList(baseSuccessResponse()));
        
        // content 배열
        fields.add(fieldWithPath("response.content").type(JsonFieldType.ARRAY).description("사용 내역 목록"));
        fields.addAll(Arrays.asList(modelUsageHistoryContentFields()));
        
        // 페이징 정보
        fields.addAll(Arrays.asList(pagingFields("response.")));
        
        return fields.toArray(new FieldDescriptor[0]);
    }

    /**
     * 모델 사용 횟수 응답 필드
     */
    public static FieldDescriptor[] modelUsageCountResponse() {
        return mergeFields(
            baseSuccessResponse(),
            new FieldDescriptor[]{
                fieldWithPath("response.totalCount").type(JsonFieldType.NUMBER).description("전체 모델 사용 횟수")
            }
        );
    }

    // ========== 인증 관련 ==========
    
    /**
     * 인증 헤더 파라미터
     */
    public static org.springframework.restdocs.headers.HeaderDescriptor[] authHeaders() {
        return new org.springframework.restdocs.headers.HeaderDescriptor[]{
            org.springframework.restdocs.headers.HeaderDocumentation.headerWithName("Authorization").description("Bearer {token}")
        };
    }

    /**
     * 회원가입 요청 필드
     */
    public static FieldDescriptor[] signUpRequestFields() {
        return new FieldDescriptor[]{
            fieldWithPath("username").type(JsonFieldType.STRING).description("사용자명 (2-20자)"),
            fieldWithPath("email").type(JsonFieldType.STRING).description("이메일 주소"),
            fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호 (4-20자)")
        };
    }

    /**
     * 로그인 요청 필드
     */
    public static FieldDescriptor[] loginRequestFields() {
        return new FieldDescriptor[]{
            fieldWithPath("email").type(JsonFieldType.STRING).description("이메일 주소"),
            fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호")
        };
    }

    /**
     * 인증 성공 응답 필드 (메시지만)
     */
    public static FieldDescriptor[] authSuccessResponse() {
        return new FieldDescriptor[]{
            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
            fieldWithPath("response").type(JsonFieldType.STRING).description("성공 메시지"),
            fieldWithPath("error").type(JsonFieldType.NULL).description("에러 정보 (성공시 null)").optional()
        };
    }

    /**
     * 회원가입 성공 응답 필드 (response가 null)
     */
    public static FieldDescriptor[] signUpSuccessResponse() {
        return new FieldDescriptor[]{
            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
            fieldWithPath("response").type(JsonFieldType.NULL).description("응답 데이터 (회원가입시 null)").optional(),
            fieldWithPath("error").type(JsonFieldType.NULL).description("에러 정보 (성공시 null)").optional()
        };
    }

    // ========== 유틸리티 메서드 ==========
    
    /**
     * 여러 필드 배열을 하나로 병합
     */
    public static FieldDescriptor[] mergeFields(FieldDescriptor[]... fieldArrays) {
        List<FieldDescriptor> merged = new ArrayList<>();
        for (FieldDescriptor[] fields : fieldArrays) {
            merged.addAll(Arrays.asList(fields));
        }
        return merged.toArray(new FieldDescriptor[0]);
    }

    /**
     * 여러 파라미터 배열을 하나로 병합
     */
    public static ParameterDescriptor[] mergeParams(ParameterDescriptor[]... paramArrays) {
        List<ParameterDescriptor> merged = new ArrayList<>();
        for (ParameterDescriptor[] params : paramArrays) {
            merged.addAll(Arrays.asList(params));
        }
        return merged.toArray(new ParameterDescriptor[0]);
    }

    /**
     * 기존 필드 배열에 추가 필드를 더함
     */
    public static FieldDescriptor[] addFields(FieldDescriptor[] base, FieldDescriptor... additional) {
        List<FieldDescriptor> list = new ArrayList<>(Arrays.asList(base));
        list.addAll(Arrays.asList(additional));
        return list.toArray(new FieldDescriptor[0]);
    }

    /**
     * 기존 파라미터 배열에 추가 파라미터를 더함
     */
    public static ParameterDescriptor[] addParams(ParameterDescriptor[] base, ParameterDescriptor... additional) {
        List<ParameterDescriptor> list = new ArrayList<>(Arrays.asList(base));
        list.addAll(Arrays.asList(additional));
        return list.toArray(new ParameterDescriptor[0]);
    }
}