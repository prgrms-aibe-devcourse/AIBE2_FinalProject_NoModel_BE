# 테스트 코드 작성 가이드

## 🧪 테스트 환경 설정

### 컨트롤러 테스트 기본 설정

```java
@WebMvcTest(controllers = YourController.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@DisplayName("YourController 단위 테스트")
class YourControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private YourService yourService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JWTTokenProvider jwtTokenProvider;
}
```

## 📚 REST Docs 문서화

### 기본 Import 문

```java
import static com.example.nomodel._core.restdocs.RestDocsConfig.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
```

### 1. 단순 응답 API 문서화

```java
@Test
@DisplayName("단순 응답 테스트")
@WithMockUser
void simpleResponse_Success() throws Exception {
    // Given
    YourResponse response = YourResponse.builder()
        .field1("value1")
        .field2("value2")
        .build();
    
    given(yourService.getData()).willReturn(response);
    
    // When & Then
    mockMvc.perform(get("/api/simple"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andDo(document("simple-response",
            preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            responseFields(mergeFields(
                baseSuccessResponse(),
                new FieldDescriptor[]{
                    fieldWithPath("response.field1").description("필드1 설명"),
                    fieldWithPath("response.field2").description("필드2 설명")
                }
            ))
        ));
}
```

### 2. 페이징 API 문서화

```java
@Test
@DisplayName("페이징 응답 테스트")
@WithMockUser
void pagingResponse_Success() throws Exception {
    // Given
    YourPageResponse pageResponse = YourPageResponse.builder()
        .content(Arrays.asList(/* 데이터 */))
        .pageNumber(0)
        .pageSize(20)
        .totalElements(100L)
        .totalPages(5)
        .hasNext(true)
        .hasPrevious(false)
        .build();
    
    given(yourService.getPagedData(0, 20)).willReturn(pageResponse);
    
    // When & Then
    mockMvc.perform(get("/api/paged")
            .param("page", "0")
            .param("size", "20"))
        .andDo(print())
        .andExpect(status().isOk())
        .andDo(document("paged-response",
            preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            queryParameters(pagingParams()),
            responseFields(mergeFields(
                baseSuccessResponse(),
                new FieldDescriptor[]{
                    fieldWithPath("response.content").type(JsonFieldType.ARRAY).description("데이터 목록"),
                    fieldWithPath("response.content[].field1").description("아이템 필드1"),
                    fieldWithPath("response.content[].field2").description("아이템 필드2")
                },
                pagingFields("response.")
            ))
        ));
}
```

### 3. 에러 응답 문서화

```java
@Test
@DisplayName("에러 응답 테스트")
@WithMockUser
void errorResponse_BadRequest() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/error")
            .param("invalidParam", "invalid"))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andDo(document("error-response",
            preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            responseFields(baseErrorResponse())
        ));
}
```

## 🔧 공통 유틸리티 사용법

### RestDocsConfig 주요 메서드

| 메서드 | 용도 | 예시 |
|--------|------|------|
| `baseSuccessResponse()` | 성공 응답 기본 구조 | success, response, error |
| `baseErrorResponse()` | 에러 응답 기본 구조 | success, response, error.status |
| `pagingParams()` | 페이징 파라미터 | page, size |
| `pagingFields(prefix)` | 페이징 응답 필드 | pageNumber, totalElements 등 |
| `mergeFields(...)` | 필드 배열 병합 | 여러 필드 그룹 합치기 |
| `addFields(base, ...)` | 기존 필드에 추가 | 기본 + 추가 필드 |

### 커스텀 필드 정의

```java
// 도메인별 공통 필드 정의
public static FieldDescriptor[] userFields(String prefix) {
    return new FieldDescriptor[]{
        fieldWithPath(prefix + "userId").description("사용자 ID"),
        fieldWithPath(prefix + "email").description("이메일"),
        fieldWithPath(prefix + "name").description("이름"),
        fieldWithPath(prefix + "createdAt").description("생성일시")
    };
}

// 사용 예시
responseFields(mergeFields(
    baseSuccessResponse(),
    userFields("response.")
));
```

## 📝 테스트 패턴별 템플릿

### 1. GET API (단일 조회)

```java
@Test
@DisplayName("단일 조회 - 성공")
@WithMockUser
void getSingle_Success() throws Exception {
    // Given
    Long id = 1L;
    YourEntity entity = createTestEntity(id);
    given(yourService.findById(id)).willReturn(entity);
    
    // When & Then
    mockMvc.perform(get("/api/entity/{id}", id))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.response.id").value(id))
        .andDo(document("get-entity",
            pathParameters(
                parameterWithName("id").description("엔티티 ID")
            ),
            responseFields(/* 필드 정의 */)
        ));
    
    verify(yourService).findById(id);
}
```

### 2. POST API (생성)

```java
@Test
@DisplayName("생성 - 성공")
@WithMockUser
void create_Success() throws Exception {
    // Given
    CreateRequest request = CreateRequest.builder()
        .field1("value1")
        .field2("value2")
        .build();
    
    YourEntity created = createTestEntity(1L);
    given(yourService.create(any())).willReturn(created);
    
    // When & Then
    mockMvc.perform(post("/api/entity")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andDo(document("create-entity",
            requestFields(
                fieldWithPath("field1").description("필드1"),
                fieldWithPath("field2").description("필드2")
            ),
            responseFields(/* 응답 필드 */)
        ));
}
```

### 3. PUT API (수정)

```java
@Test
@DisplayName("수정 - 성공")
@WithMockUser
void update_Success() throws Exception {
    // Given
    Long id = 1L;
    UpdateRequest request = UpdateRequest.builder()
        .field1("updated1")
        .field2("updated2")
        .build();
    
    YourEntity updated = createTestEntity(id);
    given(yourService.update(eq(id), any())).willReturn(updated);
    
    // When & Then
    mockMvc.perform(put("/api/entity/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andDo(document("update-entity",
            pathParameters(
                parameterWithName("id").description("엔티티 ID")
            ),
            requestFields(/* 요청 필드 */),
            responseFields(/* 응답 필드 */)
        ));
}
```

### 4. DELETE API (삭제)

```java
@Test
@DisplayName("삭제 - 성공")
@WithMockUser
void delete_Success() throws Exception {
    // Given
    Long id = 1L;
    doNothing().when(yourService).delete(id);
    
    // When & Then
    mockMvc.perform(delete("/api/entity/{id}", id))
        .andDo(print())
        .andExpect(status().isNoContent())
        .andDo(document("delete-entity",
            pathParameters(
                parameterWithName("id").description("삭제할 엔티티 ID")
            )
        ));
    
    verify(yourService).delete(id);
}
```

## 🔒 인증/인가 테스트

### 인증된 사용자 테스트

```java
@Test
@DisplayName("인증된 사용자 - 성공")
@WithMockUser
void authenticatedUser_Success() throws Exception {
    // 테스트 로직
}

// 또는 커스텀 사용자 정보
@Test
@DisplayName("커스텀 사용자 - 성공")
void customUser_Success() throws Exception {
    mockMvc.perform(get("/api/secured")
            .with(user(createCustomUserDetails(1L))))
        .andExpect(status().isOk());
}
```

### 인증되지 않은 사용자 테스트

```java
@Test
@DisplayName("인증되지 않은 사용자 - 리다이렉트")
void unauthenticatedUser_Redirect() throws Exception {
    mockMvc.perform(get("/api/secured"))
        .andDo(print())
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("http://localhost/login"));
    
    verifyNoInteractions(yourService);
}
```

## 🛠️ 테스트 헬퍼 메서드

### CustomUserDetails 생성

```java
private CustomUserDetails createCustomUserDetails(Long memberId) {
    return new CustomUserDetails(
        memberId,
        "test@example.com",
        "encodedPassword",
        Collections.emptyList()
    );
}
```

### 테스트 데이터 생성

```java
private YourEntity createTestEntity(Long id) {
    return YourEntity.builder()
        .id(id)
        .field1("test1")
        .field2("test2")
        .createdAt(LocalDateTime.now())
        .build();
}

private List<YourEntity> createTestEntities(int count) {
    return IntStream.range(1, count + 1)
        .mapToObj(i -> createTestEntity((long) i))
        .collect(Collectors.toList());
}
```

## 📋 체크리스트

### 테스트 작성 시 확인사항

- [ ] `@DisplayName`으로 테스트 목적 명시
- [ ] Given-When-Then 패턴 사용
- [ ] 적절한 Mock 설정
- [ ] 응답 검증 (status, jsonPath)
- [ ] REST Docs 문서화
- [ ] Service 메서드 호출 검증 (`verify`)
- [ ] 예외 상황 테스트
- [ ] 인증/인가 테스트

### REST Docs 문서화 체크리스트

- [ ] 요청 파라미터 문서화
- [ ] 요청 본문 문서화 (POST/PUT)
- [ ] 응답 필드 문서화
- [ ] 에러 응답 문서화
- [ ] 한글 설명 작성
- [ ] 필수/선택 필드 표시

## 🚀 실행 명령어

```bash
# 단일 테스트 실행
./gradlew test --tests "YourControllerTest.methodName"

# 클래스 전체 테스트
./gradlew test --tests "YourControllerTest"

# REST Docs 생성
./gradlew asciidoctor

# 전체 테스트 + 문서 생성
./gradlew clean test asciidoctor
```

## 💡 팁

1. **테스트 이름 규칙**: `메서드명_조건_결과` (예: `getUser_ValidId_Success`)
2. **Mock 최소화**: 테스트 대상이 아닌 의존성만 Mock
3. **데이터 독립성**: 각 테스트는 독립적으로 실행 가능해야 함
4. **문서화 우선**: REST Docs는 테스트와 함께 작성
5. **예외 케이스**: 정상 케이스뿐만 아니라 예외 상황도 테스트

---

이 가이드를 참고하여 일관성 있고 문서화가 잘 된 테스트 코드를 작성하세요! 🎯