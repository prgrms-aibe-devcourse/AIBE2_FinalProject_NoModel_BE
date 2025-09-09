package com.example.nomodel._core.config;

import org.springframework.boot.test.autoconfigure.restdocs.RestDocsMockMvcConfigurationCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;

/**
 * Spring REST Docs 공통 설정
 * 모든 REST Docs 테스트에서 일관된 문서화 형식을 제공
 */
@TestConfiguration
public class RestDocsConfiguration {

    /**
     * REST Docs MockMvc 설정 커스터마이저
     * API 문서의 기본 URI와 포트를 설정
     */
    @Bean
    public RestDocsMockMvcConfigurationCustomizer restDocsMockMvcConfigurationCustomizer() {
        return configurer -> configurer
                .uris()
                .withScheme("https")
                .withHost("api.nomodel.com")
                .withPort(443);
    }

    /**
     * 공통 문서화 핸들러
     * 모든 API 문서에 일관된 포맷팅 적용
     */
    public static RestDocumentationResultHandler document(String identifier) {
        return MockMvcRestDocumentation.document(identifier,
                preprocessRequest(
                        modifyUris()
                                .scheme("https")
                                .host("api.nomodel.com")
                                .removePort(),
                        prettyPrint()
                ),
                preprocessResponse(
                        prettyPrint(),
                        removeHeaders("X-Content-Type-Options",
                                "X-XSS-Protection",
                                "Cache-Control",
                                "Pragma",
                                "Expires",
                                "X-Frame-Options")
                )
        );
    }

    /**
     * 에러 응답 문서화 핸들러
     * 에러 응답에 특화된 포맷팅 적용
     */
    public static RestDocumentationResultHandler documentError(String identifier) {
        return MockMvcRestDocumentation.document(identifier,
                preprocessRequest(prettyPrint()),
                preprocessResponse(
                        prettyPrint(),
                        removeHeaders("X-Content-Type-Options",
                                "X-XSS-Protection",
                                "Cache-Control",
                                "Pragma",
                                "Expires",
                                "X-Frame-Options",
                                "Content-Length")
                )
        );
    }

    public static FieldDescriptor[] searchSuccessResponse() {
        return new FieldDescriptor[]{
            fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
            fieldWithPath("response").type(JsonFieldType.OBJECT).description("응답 데이터"),
            fieldWithPath("response.content").type(JsonFieldType.ARRAY).description("검색 결과 목록"),
            fieldWithPath("response.content[].id").type(JsonFieldType.STRING).description("모델 문서 ID").optional(),
            fieldWithPath("response.content[].modelId").type(JsonFieldType.NUMBER).description("모델 ID").optional(),
            fieldWithPath("response.content[].modelName").type(JsonFieldType.STRING).description("모델 이름").optional(),
            fieldWithPath("response.content[].suggest").type(JsonFieldType.ARRAY).description("추천 검색어").optional(),
            fieldWithPath("response.content[].prompt").type(JsonFieldType.STRING).description("모델 프롬프트").optional(),
            fieldWithPath("response.content[].tags").type(JsonFieldType.VARIES).description("태그 정보").optional(),
            fieldWithPath("response.content[].ownType").type(JsonFieldType.VARIES).description("소유 유형").optional(),
            fieldWithPath("response.content[].ownerId").type(JsonFieldType.NUMBER).description("소유자 ID").optional(),
            fieldWithPath("response.content[].ownerName").type(JsonFieldType.VARIES).description("소유자 이름").optional(),
            fieldWithPath("response.content[].price").type(JsonFieldType.VARIES).description("가격").optional(),
            fieldWithPath("response.content[].isPublic").type(JsonFieldType.BOOLEAN).description("공개 여부").optional(),
            fieldWithPath("response.content[].usageCount").type(JsonFieldType.NUMBER).description("사용 횟수").optional(),
            fieldWithPath("response.content[].viewCount").type(JsonFieldType.NUMBER).description("조회 횟수").optional(),
            fieldWithPath("response.content[].rating").type(JsonFieldType.NUMBER).description("평점").optional(),
            fieldWithPath("response.content[].reviewCount").type(JsonFieldType.NUMBER).description("리뷰 수").optional(),
            fieldWithPath("response.content[].createdAt").type(JsonFieldType.VARIES).description("생성 시간").optional(),
            fieldWithPath("response.content[].updatedAt").type(JsonFieldType.VARIES).description("수정 시간").optional(),
            fieldWithPath("response.page").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
            fieldWithPath("response.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
            fieldWithPath("response.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
            fieldWithPath("response.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
            fieldWithPath("response.first").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
            fieldWithPath("response.last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
            fieldWithPath("response.empty").type(JsonFieldType.BOOLEAN).description("빈 페이지 여부"),
            fieldWithPath("error").type(JsonFieldType.NULL).description("에러 정보 (성공시 null)").optional()
        };
    }
}