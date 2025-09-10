package com.example.nomodel._core.restdocs;

import com.example.nomodel._core.config.RestDocsConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REST Docs 테스트 템플릿
 * 
 * 이 템플릿을 복사하여 새로운 컨트롤러 테스트를 작성하세요.
 * 
 * 필수 요소:
 * 1. @WebMvcTest(controllers = YourController.class) - 테스트할 컨트롤러 지정
 * 2. @AutoConfigureRestDocs - REST Docs 자동 설정
 * 3. @AutoConfigureMockMvc(addFilters = false) - Security 필터 비활성화 (선택적)
 * 4. @ContextConfiguration(classes = RestDocsConfiguration.class) - REST Docs 설정 로드 (선택적)
 * 
 * 사용 예시:
 * <pre>
 * {@code
 * @WebMvcTest(controllers = YourController.class)
 * @AutoConfigureRestDocs
 * @AutoConfigureMockMvc(addFilters = false)
 * @ContextConfiguration(classes = RestDocsConfiguration.class)
 * class YourControllerTest {
 *     
 *     @Autowired
 *     private MockMvc mockMvc;
 *     
 *     @Autowired
 *     private ObjectMapper objectMapper;
 *     
 *     @MockitoBean
 *     private YourService yourService;
 *     
 *     @Test
 *     void testMethod() throws Exception {
 *         // given
 *         RequestDto request = new RequestDto(...);
 *         ResponseDto response = new ResponseDto(...);
 *         given(yourService.method(any())).willReturn(response);
 *         
 *         // when & then
 *         mockMvc.perform(post("/api/endpoint")
 *                 .contentType(MediaType.APPLICATION_JSON)
 *                 .content(objectMapper.writeValueAsString(request)))
 *             .andDo(print())
 *             .andExpect(status().isOk())
 *             .andDo(document("snippet-name",
 *                 preprocessRequest(prettyPrint()),
 *                 preprocessResponse(prettyPrint()),
 *                 requestFields(
 *                     fieldWithPath("field1").description("필드 설명"),
 *                     fieldWithPath("field2").description("필드 설명")
 *                 ),
 *                 responseFields(
 *                     fieldWithPath("success").description("성공 여부"),
 *                     fieldWithPath("response").description("응답 데이터"),
 *                     fieldWithPath("error").description("에러 정보").optional()
 *                 )
 *             ));
 *     }
 * }
 * }
 * </pre>
 */
public abstract class RestDocsTestTemplate {
    
    /**
     * 기본 요청 필드 문서화
     * 
     * 사용 예시:
     * requestFields(
     *     fieldWithPath("name").type(JsonFieldType.STRING).description("이름"),
     *     fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
     *     fieldWithPath("age").type(JsonFieldType.NUMBER).description("나이").optional()
     * )
     */
    protected void documentRequestFields() {
        // 템플릿 메소드
    }
    
    /**
     * 기본 응답 필드 문서화
     * 
     * 사용 예시:
     * responseFields(
     *     fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
     *     fieldWithPath("response").type(JsonFieldType.OBJECT).description("응답 데이터"),
     *     fieldWithPath("response.id").type(JsonFieldType.NUMBER).description("ID"),
     *     fieldWithPath("error").type(JsonFieldType.NULL).description("에러 정보").optional()
     * )
     */
    protected void documentResponseFields() {
        // 템플릿 메소드
    }
    
    /**
     * 쿼리 파라미터 문서화
     * 
     * 사용 예시:
     * queryParameters(
     *     parameterWithName("page").description("페이지 번호").optional(),
     *     parameterWithName("size").description("페이지 크기").optional(),
     *     parameterWithName("sort").description("정렬 기준").optional()
     * )
     */
    protected void documentQueryParameters() {
        // 템플릿 메소드
    }
    
    /**
     * 경로 변수 문서화
     * 
     * 사용 예시:
     * pathParameters(
     *     parameterWithName("id").description("리소스 ID")
     * )
     */
    protected void documentPathParameters() {
        // 템플릿 메소드
    }
    
    /**
     * 요청 헤더 문서화
     * 
     * 사용 예시:
     * requestHeaders(
     *     headerWithName("Authorization").description("인증 토큰"),
     *     headerWithName("X-Request-ID").description("요청 ID").optional()
     * )
     */
    protected void documentRequestHeaders() {
        // 템플릿 메소드
    }
    
    /**
     * 응답 헤더 문서화
     * 
     * 사용 예시:
     * responseHeaders(
     *     headerWithName("X-Total-Count").description("전체 개수"),
     *     headerWithName("X-Page-Count").description("페이지 수")
     * )
     */
    protected void documentResponseHeaders() {
        // 템플릿 메소드
    }
}