# REST Docs 테스트 작성 가이드

## 1. 필수 애노테이션

모든 REST Docs 테스트는 다음 애노테이션들을 포함해야 합니다:

```java
@WebMvcTest(controllers = YourController.class)  // 테스트할 컨트롤러 지정
@AutoConfigureRestDocs                           // REST Docs 자동 설정
@AutoConfigureMockMvc(addFilters = false)        // Security 필터 비활성화 (선택적)
@ContextConfiguration(classes = RestDocsConfiguration.class) // REST Docs 설정 로드 (선택적)
```

## 2. 기본 구조

```java
@WebMvcTest(controllers = ExampleController.class)
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false)
class ExampleControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockitoBean
    private ExampleService exampleService;
    
    @Test
    @DisplayName("API 테스트 설명")
    void testMethod() throws Exception {
        // given - 테스트 데이터 준비
        RequestDto request = new RequestDto(...);
        ResponseDto response = new ResponseDto(...);
        given(exampleService.method(any())).willReturn(response);
        
        // when & then - 요청 실행 및 검증
        mockMvc.perform(post("/api/endpoint")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())  // 요청/응답 콘솔 출력
            .andExpect(status().isOk())  // 상태 코드 검증
            .andExpect(jsonPath("$.success").value(true))  // 응답 내용 검증
            .andDo(document("snippet-name",  // REST Docs 스니펫 생성
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(...),
                responseFields(...)
            ));
    }
}
```

## 3. 필수 import 문

```java
// REST Docs 관련
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;

// 공통 설정
import static com.example.nomodel._core.restdocs.RestDocsConfig.*;

// MockMvc 테스트
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Mockito
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
```

## 4. 문서화 유형별 예제

### 4.1. GET 요청 (쿼리 파라미터)

```java
mockMvc.perform(get("/api/search")
        .param("keyword", "test")
        .param("page", "0")
        .param("size", "10"))
    .andDo(document("search",
        queryParameters(
            parameterWithName("keyword").description("검색 키워드"),
            parameterWithName("page").description("페이지 번호").optional(),
            parameterWithName("size").description("페이지 크기").optional()
        ),
        responseFields(searchSuccessResponse())
    ));
```

### 4.2. POST 요청 (Request Body)

```java
mockMvc.perform(post("/api/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(signUpDto)))
    .andDo(document("signup",
        requestFields(
            fieldWithPath("username").description("사용자명"),
            fieldWithPath("email").description("이메일"),
            fieldWithPath("password").description("비밀번호")
        ),
        responseFields(baseSuccessResponse())
    ));
```

### 4.3. 경로 변수 (Path Variable)

```java
mockMvc.perform(get("/api/users/{id}", 1L))
    .andDo(document("get-user",
        pathParameters(
            parameterWithName("id").description("사용자 ID")
        ),
        responseFields(...)
    ));
```

### 4.4. 헤더 문서화

```java
mockMvc.perform(post("/api/refresh")
        .header("Authorization", "Bearer token"))
    .andDo(document("refresh",
        requestHeaders(
            headerWithName("Authorization").description("인증 토큰")
        ),
        responseFields(...)
    ));
```

## 5. 공통 헬퍼 메소드 활용

`RestDocsConfig` 클래스에서 제공하는 공통 메소드들을 활용하세요:

```java
// 기본 응답 구조
baseSuccessResponse()  // success, response, error 필드
baseErrorResponse()    // 에러 응답 필드

// 페이징
pagingParams()         // page, size 파라미터
pagingFields("response.")  // 페이징 응답 필드

// 인증
authHeaders()          // Authorization 헤더
loginRequestFields()   // 로그인 요청 필드
signUpRequestFields()  // 회원가입 요청 필드

// 필드 병합
mergeFields(baseSuccessResponse(), customFields)
```

## 6. 스니펫 네이밍 규칙

- **리소스-액션** 형식 사용: `member-signup`, `model-search`, `auth-login`
- 소문자와 하이픈 사용
- RESTful 리소스명 사용

## 7. 체크리스트

- [ ] `@WebMvcTest` 애노테이션 추가
- [ ] `@AutoConfigureRestDocs` 애노테이션 추가
- [ ] MockMvc 주입
- [ ] Service Mock 설정
- [ ] `document()` 호출 추가
- [ ] 요청 파라미터/바디 문서화
- [ ] 응답 필드 문서화
- [ ] 스니펫 이름 규칙 준수
- [ ] 테스트 실행 확인

## 8. 생성된 스니펫 확인

테스트 실행 후 다음 위치에서 생성된 스니펫을 확인할 수 있습니다:
```
build/generated-snippets/
├── {snippet-name}/
│   ├── curl-request.adoc
│   ├── http-request.adoc
│   ├── http-response.adoc
│   ├── request-fields.adoc (POST/PUT 요청)
│   ├── response-fields.adoc
│   ├── query-parameters.adoc (GET 요청)
│   └── path-parameters.adoc (경로 변수)
```

## 9. API 문서에 포함시키기

생성된 스니펫을 `src/docs/asciidoc/api-guide.adoc`에 포함시킵니다:

```asciidoc
=== API 이름
include::{snippets}/snippet-name/curl-request.adoc[]
include::{snippets}/snippet-name/http-request.adoc[]
include::{snippets}/snippet-name/request-fields.adoc[]
include::{snippets}/snippet-name/http-response.adoc[]
include::{snippets}/snippet-name/response-fields.adoc[]
```

## 10. 빌드 및 문서 생성

```bash
# 테스트 실행 및 스니펫 생성
./gradlew test

# API 문서 생성 (HTML)
./gradlew asciidoctor

# 또는 한번에 (build가 asciidoctor를 포함하도록 설정됨)
./gradlew build
```

생성된 HTML 문서는 `build/docs/asciidoc/` 디렉토리에서 확인할 수 있습니다.