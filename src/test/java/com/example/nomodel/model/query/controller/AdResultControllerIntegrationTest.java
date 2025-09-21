package com.example.nomodel.model.query.controller;

import com.example.nomodel._core.base.BaseIntegrationTest;
import com.example.nomodel._core.fixture.TestDataFixture;
import com.example.nomodel._core.security.jwt.JWTTokenProvider;
import com.example.nomodel.member.domain.model.Member;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.model.command.application.dto.request.AdResultRatingUpdateRequestDto;
import com.example.nomodel.model.command.domain.model.AdResult;
import com.example.nomodel.model.command.domain.model.AIModel;
import com.example.nomodel.model.command.domain.repository.AIModelJpaRepository;
import com.example.nomodel.model.command.domain.repository.AdResultJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@AutoConfigureRestDocs
@DisplayName("AdResult Controller 통합 테스트")
class AdResultControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private MemberJpaRepository memberRepository;
    
    @Autowired
    private AIModelJpaRepository aiModelRepository;
    
    @Autowired
    private AdResultJpaRepository adResultRepository;
    
    @Autowired
    private JWTTokenProvider jwtTokenProvider;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private Member testMember;
    private AIModel testModel;
    private AdResult testAdResult;
    private String accessToken;
    
    @BeforeEach
    void setUp() {
        // 테스트 회원 생성
        testMember = TestDataFixture.createDefaultMember(passwordEncoder);
        testMember = memberRepository.save(testMember);
        
        // JWT 토큰 생성
        accessToken = TestDataFixture.createJwtToken(testMember, jwtTokenProvider);
        
        // 테스트 AI 모델 생성
        testModel = TestDataFixture.createDefaultAIModel(testMember.getId());
        testModel = aiModelRepository.save(testModel);
        
        // 테스트 AdResult 생성
        testAdResult = AdResult.create(testModel.getId(), testMember.getId(), "Test prompt", "Test Result");
        testAdResult = adResultRepository.save(testAdResult);
    }
    
    @AfterEach
    void tearDown() {
        adResultRepository.deleteAll();
        aiModelRepository.deleteAll();
        memberRepository.deleteAll();
    }
    
    @Test
    @DisplayName("내 결과물 목록 조회 성공")
    void getMyAdResults_Success() throws Exception {
        mockMvc.perform(get("/api/ad-results/my")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.content").isArray())
                .andExpect(jsonPath("$.response.content[0].id").value(testAdResult.getId()))
                .andDo(document("ad-result-my-list",
                        requestHeaders(
                                headerWithName("Authorization").description("JWT 액세스 토큰")
                        ),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                                parameterWithName("size").description("페이지 크기").optional()
                        ),
                        responseFields(
                                fieldWithPath("success").description("API 성공 여부"),
                                fieldWithPath("error").description("오류 정보 (성공 시 null)"),
                                fieldWithPath("response.content").description("결과물 목록"),
                                fieldWithPath("response.content[].id").description("결과물 ID"),
                                fieldWithPath("response.content[].modelId").description("사용한 모델 ID"),
                                fieldWithPath("response.content[].memberId").description("회원 ID"),
                                fieldWithPath("response.content[].prompt").description("사용한 프롬프트"),
                                fieldWithPath("response.content[].adResultName").description("결과물 이름"),
                                fieldWithPath("response.content[].memberRating").description("회원 평점"),
                                fieldWithPath("response.content[].resultImageUrl").description("결과 이미지 URL"),
                                fieldWithPath("response.content[].createdAt").description("생성 일시"),
                                fieldWithPath("response.content[].updatedAt").description("수정 일시"),
                                fieldWithPath("response.pageable").description("페이지 정보"),
                                fieldWithPath("response.pageable.pageNumber").description("현재 페이지 번호"),
                                fieldWithPath("response.pageable.pageSize").description("페이지 크기"),
                                fieldWithPath("response.pageable.sort").description("정렬 정보"),
                                fieldWithPath("response.pageable.sort.empty").description("정렬 정보 존재 여부"),
                                fieldWithPath("response.pageable.sort.unsorted").description("정렬되지 않음 여부"),
                                fieldWithPath("response.pageable.sort.sorted").description("정렬됨 여부"),
                                fieldWithPath("response.pageable.offset").description("오프셋"),
                                fieldWithPath("response.pageable.unpaged").description("페이징되지 않음 여부"),
                                fieldWithPath("response.pageable.paged").description("페이징됨 여부"),
                                fieldWithPath("response.totalElements").description("전체 요소 수"),
                                fieldWithPath("response.totalPages").description("전체 페이지 수"),
                                fieldWithPath("response.size").description("페이지 크기"),
                                fieldWithPath("response.number").description("현재 페이지 번호"),
                                fieldWithPath("response.first").description("첫 번째 페이지 여부"),
                                fieldWithPath("response.last").description("마지막 페이지 여부"),
                                fieldWithPath("response.empty").description("빈 페이지 여부"),
                                fieldWithPath("response.sort").description("정렬 정보"),
                                fieldWithPath("response.sort.empty").description("정렬 정보 존재 여부"),
                                fieldWithPath("response.sort.unsorted").description("정렬되지 않음 여부"),
                                fieldWithPath("response.sort.sorted").description("정렬됨 여부"),
                                fieldWithPath("response.numberOfElements").description("현재 페이지 요소 수")
                        )
                ));
    }
    
    @Test
    @DisplayName("내 결과물 상세 조회 성공")
    void getMyAdResult_Success() throws Exception {
        mockMvc.perform(get("/api/ad-results/my/{adResultId}", testAdResult.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.id").value(testAdResult.getId()))
                .andExpect(jsonPath("$.response.modelId").value(testModel.getId()))
                .andDo(document("ad-result-detail",
                        requestHeaders(
                                headerWithName("Authorization").description("JWT 액세스 토큰")
                        ),
                        pathParameters(
                                parameterWithName("adResultId").description("결과물 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").description("API 성공 여부"),
                                fieldWithPath("error").description("오류 정보 (성공 시 null)"),
                                fieldWithPath("response.id").description("결과물 ID"),
                                fieldWithPath("response.modelId").description("사용한 모델 ID"),
                                fieldWithPath("response.memberId").description("회원 ID"),
                                fieldWithPath("response.prompt").description("사용한 프롬프트"),
                                fieldWithPath("response.adResultName").description("결과물 이름"),
                                fieldWithPath("response.memberRating").description("회원 평점"),
                                fieldWithPath("response.resultImageUrl").description("결과 이미지 URL"),
                                fieldWithPath("response.createdAt").description("생성 일시"),
                                fieldWithPath("response.updatedAt").description("수정 일시")
                        )
                ));
    }
    
    @Test
    @DisplayName("내 프로젝트 개수 조회 성공")
    void getMyProjectCount_Success() throws Exception {
        mockMvc.perform(get("/api/ad-results/my/count")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.totalProjects").value(1))
                .andDo(document("ad-result-count",
                        requestHeaders(
                                headerWithName("Authorization").description("JWT 액세스 토큰")
                        ),
                        responseFields(
                                fieldWithPath("success").description("API 성공 여부"),
                                fieldWithPath("error").description("오류 정보 (성공 시 null)"),
                                fieldWithPath("response.totalProjects").description("총 프로젝트 개수")
                        )
                ));
    }
    
    @Test
    @DisplayName("내 평균 평점 조회 성공")
    void getMyAverageRating_Success() throws Exception {
        // 평점 추가
        testAdResult.updateRating(4.5);
        adResultRepository.save(testAdResult);
        
        mockMvc.perform(get("/api/ad-results/my/average-rating")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.averageRating").value(4.5))
                .andDo(document("ad-result-average-rating",
                        requestHeaders(
                                headerWithName("Authorization").description("JWT 액세스 토큰")
                        ),
                        responseFields(
                                fieldWithPath("success").description("API 성공 여부"),
                                fieldWithPath("error").description("오류 정보 (성공 시 null)"),
                                fieldWithPath("response.averageRating").description("평균 평점")
                        )
                ));
    }
    
    @Test
    @DisplayName("결과물 평점 업데이트 성공")
    void updateAdResultRating_Success() throws Exception {
        AdResultRatingUpdateRequestDto request = new AdResultRatingUpdateRequestDto(4.0);
        
        mockMvc.perform(patch("/api/ad-results/my/{adResultId}/rating", testAdResult.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("ad-result-rating-update",
                        requestHeaders(
                                headerWithName("Authorization").description("JWT 액세스 토큰")
                        ),
                        pathParameters(
                                parameterWithName("adResultId").description("결과물 ID")
                        ),
                        requestFields(
                                fieldWithPath("memberRating").description("평점 (0.0 ~ 5.0)")
                        ),
                        responseFields(
                                fieldWithPath("success").description("API 성공 여부"),
                                fieldWithPath("error").description("오류 정보 (성공 시 null)"),
                                fieldWithPath("response").description("성공 메시지")
                        )
                ));
    }
    
    @Test
    @DisplayName("존재하지 않는 결과물 조회 실패")
    void getMyAdResult_NotFound() throws Exception {
        Long nonExistentId = 999L;
        
        mockMvc.perform(get("/api/ad-results/my/{adResultId}", nonExistentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
    
    @Test
    @DisplayName("유효하지 않은 평점으로 업데이트 실패")
    void updateAdResultRating_InvalidRating() throws Exception {
        AdResultRatingUpdateRequestDto request = new AdResultRatingUpdateRequestDto(6.0); // 유효 범위 초과
        
        mockMvc.perform(patch("/api/ad-results/my/{adResultId}/rating", testAdResult.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}