package com.example.nomodel._core.config;

import org.springframework.boot.test.autoconfigure.restdocs.RestDocsMockMvcConfigurationCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.operation.preprocess.Preprocessors;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;

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
}