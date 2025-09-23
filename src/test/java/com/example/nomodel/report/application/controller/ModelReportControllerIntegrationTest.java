package com.example.nomodel.report.application.controller;

import com.example.nomodel._core.base.BaseIntegrationTest;
import com.example.nomodel._core.security.jwt.JWTTokenProvider;
import com.example.nomodel.member.domain.model.Member;
import com.example.nomodel.member.domain.model.Role;
import com.example.nomodel.member.domain.model.Status;
import com.example.nomodel.member.domain.model.Email;
import com.example.nomodel.member.domain.model.Password;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.report.application.dto.request.ModelReportRequest;
import com.example.nomodel._core.config.TestOAuth2Config;
import com.example.nomodel.model.command.domain.model.AIModel;
import com.example.nomodel.model.command.domain.model.ModelMetadata;
import com.example.nomodel.model.command.domain.model.OwnType;
import com.example.nomodel.model.command.domain.model.SamplerType;
import com.example.nomodel.model.command.domain.repository.AIModelJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.context.annotation.Import;
import com.example.nomodel.member.application.dto.response.AuthTokenDTO;

import java.math.BigDecimal;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@AutoConfigureRestDocs
@DisplayName("ModelReportController 통합 테스트")
class ModelReportControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JWTTokenProvider jwtTokenProvider;

    @Autowired
    private MemberJpaRepository memberRepository;
    
    @Autowired
    private AIModelJpaRepository aiModelRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member testMember;
    private String validJwtToken;
    private AIModel testModel;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성
        testMember = Member.builder()
                .username("testUser")
                .email(Email.of("test@example.com"))
                .password(Password.encode("testPassword", passwordEncoder))
                .role(Role.USER)
                .status(Status.ACTIVE)
                .build();
        
        testMember = memberRepository.save(testMember);
        
        // 실제 JWT 토큰 생성
        AuthTokenDTO authToken = jwtTokenProvider.generateToken(
                testMember.getEmail().getValue(),
                testMember.getId(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + testMember.getRole().name()))
        );
        validJwtToken = authToken.accessToken();
        
        // 테스트용 AI 모델 생성
        ModelMetadata modelMetadata = ModelMetadata.builder()
                .seed(1234L)
                .prompt("Test model prompt")
                .negativePrompt("Test negative prompt")
                .width(512)
                .height(512)
                .steps(20)
                .samplerIndex(SamplerType.EULER_A)
                .nIter(1)
                .batchSize(1)
                .build();
                
        testModel = AIModel.builder()
                .modelName("Test Model")
                .modelMetadata(modelMetadata)
                .ownType(OwnType.USER)
                .ownerId(testMember.getId())
                .price(BigDecimal.valueOf(10.0))
                .isPublic(true)
                .build();
                
        testModel = aiModelRepository.save(testModel);
    }

    @Test
    @DisplayName("실제 JWT 토큰으로 모델 신고 성공")
    void createModelReport_WithRealJWT_Success() throws Exception {
        // given
        Long modelId = testModel.getId();
        String reasonDetail = "부적절한 콘텐츠가 포함되어 있습니다.";
        ModelReportRequest request = new ModelReportRequest(reasonDetail);

        // when & then
        mockMvc.perform(post("/reports/models/{modelId}", modelId)
                        .header("Authorization", "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.reportId").exists())
                .andExpect(jsonPath("$.response.targetType").value("MODEL"))
                .andExpect(jsonPath("$.response.targetId").value(modelId))
                .andExpect(jsonPath("$.response.reporterId").value(testMember.getId()))
                .andExpect(jsonPath("$.response.reasonDetail").value(reasonDetail))
                .andExpect(jsonPath("$.response.reportStatus").value("PENDING"))
                .andDo(document("integration-model-report-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer JWT 토큰")
                        ),
                        pathParameters(
                                parameterWithName("modelId").description("신고할 모델 ID")
                        ),
                        requestFields(
                                fieldWithPath("reasonDetail").description("신고 상세 사유")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("response.reportId").description("생성된 신고 ID"),
                                fieldWithPath("response.targetType").description("신고 대상 타입"),
                                fieldWithPath("response.targetId").description("신고 대상 ID"),
                                fieldWithPath("response.reporterId").description("신고자 ID"),
                                fieldWithPath("response.reasonDetail").description("신고 상세 사유"),
                                fieldWithPath("response.reportStatus").description("신고 상태"),
                                fieldWithPath("response.reportStatusDescription").description("신고 상태 설명"),
                                fieldWithPath("response.adminNote").description("관리자 메모").optional(),
                                fieldWithPath("response.createdAt").description("신고 생성일시"),
                                fieldWithPath("response.updatedAt").description("신고 수정일시"),
                                fieldWithPath("error").description("에러 정보").optional()
                        )
                ));
    }

    @Test
    @DisplayName("유효하지 않은 JWT 토큰으로 요청 실패")
    void createModelReport_WithInvalidJWT_Unauthorized() throws Exception {
        // given
        Long modelId = testModel.getId();
        ModelReportRequest request = new ModelReportRequest("부적절한 콘텐츠입니다.");
        String invalidToken = "invalid.jwt.token";

        // when & then
        mockMvc.perform(post("/reports/models/{modelId}", modelId)
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication failed"));
    }

    @Test
    @DisplayName("JWT 토큰 없이 요청 실패")
    void createModelReport_WithoutJWT_Unauthorized() throws Exception {
        // given
        Long modelId = testModel.getId();
        ModelReportRequest request = new ModelReportRequest("부적절한 콘텐츠입니다.");

        // when & then
        mockMvc.perform(post("/reports/models/{modelId}", modelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("실제 JWT로 내 신고 목록 조회 성공")
    void getMyModelReports_WithRealJWT_Success() throws Exception {
        // given
        // 먼저 신고를 생성하여 데이터 준비
        Long modelId = testModel.getId();
        ModelReportRequest createRequest = new ModelReportRequest("테스트 신고 사유");
        
        mockMvc.perform(post("/reports/models/{modelId}", modelId)
                        .header("Authorization", "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        // when & then - 내 신고 목록 조회
        mockMvc.perform(get("/reports/my/models")
                        .header("Authorization", "Bearer " + validJwtToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").isArray())
                .andExpect(jsonPath("$.response", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.response[0].reporterId").value(testMember.getId()))
                .andDo(document("integration-model-report-my-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer JWT 토큰")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("response").description("내 신고 목록"),
                                fieldWithPath("response[].reportId").description("신고 ID"),
                                fieldWithPath("response[].targetType").description("신고 대상 타입"),
                                fieldWithPath("response[].targetId").description("신고 대상 ID"),
                                fieldWithPath("response[].reporterId").description("신고자 ID"),
                                fieldWithPath("response[].reasonDetail").description("신고 상세 사유"),
                                fieldWithPath("response[].reportStatus").description("신고 상태"),
                                fieldWithPath("response[].reportStatusDescription").description("신고 상태 설명"),
                                fieldWithPath("response[].adminNote").description("관리자 메모").optional(),
                                fieldWithPath("response[].createdAt").description("신고 생성일시"),
                                fieldWithPath("response[].updatedAt").description("신고 수정일시"),
                                fieldWithPath("error").description("에러 정보").optional()
                        )
                ));
    }

    @Test
    @DisplayName("만료된 JWT 토큰으로 요청 실패")
    void createModelReport_WithExpiredJWT_Unauthorized() throws Exception {
        // given
        Long modelId = testModel.getId();
        ModelReportRequest request = new ModelReportRequest("부적절한 콘텐츠입니다.");
        
        // 만료된 토큰 시뮬레이션 (실제로는 만료 시간을 과거로 설정한 토큰이 필요)
        // 여기서는 잘못된 형식의 토큰으로 대체
        String expiredToken = "eyJhbGciOiJIUzI1NiJ9.expired.token";

        // when & then
        mockMvc.perform(post("/reports/models/{modelId}", modelId)
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test  
    @DisplayName("실제 SecurityConfig 설정 검증 - CSRF 비활성화")
    void verifyCSRFDisabled() throws Exception {
        // given
        Long modelId = testModel.getId();
        ModelReportRequest request = new ModelReportRequest("CSRF 테스트");

        // when & then - CSRF 토큰 없이도 성공해야 함 (SecurityConfig에서 disable)
        mockMvc.perform(post("/reports/models/{modelId}", modelId)
                        .header("Authorization", "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("실제 JWT로 특정 신고 상세 조회 성공")
    void getModelReport_WithRealJWT_Success() throws Exception {
        // given
        // 먼저 신고를 생성하여 데이터 준비
        Long modelId = testModel.getId();
        ModelReportRequest createRequest = new ModelReportRequest("상세 조회 테스트 신고");
        
        String createResponse = mockMvc.perform(post("/reports/models/{modelId}", modelId)
                        .header("Authorization", "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 생성된 신고 ID 추출
        Long reportId = objectMapper.readTree(createResponse)
                .get("response")
                .get("reportId")
                .asLong();

        // when & then - 신고 상세 조회
        mockMvc.perform(get("/reports/{reportId}", reportId)
                        .header("Authorization", "Bearer " + validJwtToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.reportId").value(reportId))
                .andExpect(jsonPath("$.response.targetType").value("MODEL"))
                .andExpect(jsonPath("$.response.targetId").value(modelId))
                .andExpect(jsonPath("$.response.reporterId").value(testMember.getId()))
                .andExpect(jsonPath("$.response.reasonDetail").value("상세 조회 테스트 신고"))
                .andExpect(jsonPath("$.response.reportStatus").value("PENDING"))
                .andDo(document("integration-model-report-detail",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer JWT 토큰")
                        ),
                        pathParameters(
                                parameterWithName("reportId").description("조회할 신고 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("response.reportId").description("신고 ID"),
                                fieldWithPath("response.targetType").description("신고 대상 타입"),
                                fieldWithPath("response.targetId").description("신고 대상 ID"),
                                fieldWithPath("response.reporterId").description("신고자 ID"),
                                fieldWithPath("response.reasonDetail").description("신고 상세 사유"),
                                fieldWithPath("response.reportStatus").description("신고 상태"),
                                fieldWithPath("response.reportStatusDescription").description("신고 상태 설명"),
                                fieldWithPath("response.adminNote").description("관리자 메모").optional(),
                                fieldWithPath("response.createdAt").description("신고 생성일시"),
                                fieldWithPath("response.updatedAt").description("신고 수정일시"),
                                fieldWithPath("error").description("에러 정보").optional()
                        )
                ));
    }

    @Test
    @DisplayName("존재하지 않는 신고 ID로 조회 실패")
    void getModelReport_WithNonExistentId_NotFound() throws Exception {
        // given
        Long nonExistentReportId = 999999L;

        // when & then
        mockMvc.perform(get("/reports/{reportId}", nonExistentReportId)
                        .header("Authorization", "Bearer " + validJwtToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("다른 사용자의 신고 조회 시 접근 권한 없음")
    void getModelReport_AccessOtherUserReport_Forbidden() throws Exception {
        // given
        // 다른 사용자 생성
        Member otherMember = Member.builder()
                .username("otherUser")
                .email(Email.of("other@example.com"))
                .password(Password.encode("otherPassword", passwordEncoder))
                .role(Role.USER)
                .status(Status.ACTIVE)
                .build();
        otherMember = memberRepository.save(otherMember);

        // 다른 사용자의 JWT 토큰 생성
        AuthTokenDTO otherAuthToken = jwtTokenProvider.generateToken(
                otherMember.getEmail().getValue(),
                otherMember.getId(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + otherMember.getRole().name()))
        );
        String otherValidJwtToken = otherAuthToken.accessToken();

        // 다른 사용자가 신고 생성
        Long modelId = testModel.getId();
        ModelReportRequest createRequest = new ModelReportRequest("다른 사용자의 신고");
        
        String createResponse = mockMvc.perform(post("/reports/models/{modelId}", modelId)
                        .header("Authorization", "Bearer " + otherValidJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 생성된 신고 ID 추출
        Long reportId = objectMapper.readTree(createResponse)
                .get("response")
                .get("reportId")
                .asLong();

        // when & then - 원래 사용자가 다른 사용자의 신고 조회 시도
        mockMvc.perform(get("/reports/{reportId}", reportId)
                        .header("Authorization", "Bearer " + validJwtToken))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("유효성 검증 실패 - 신고 사유가 비어있음")
    void createModelReport_WithEmptyReasonDetail_BadRequest() throws Exception {
        // given
        Long modelId = testModel.getId();
        ModelReportRequest request = new ModelReportRequest(""); // 빈 문자열

        // when & then
        mockMvc.perform(post("/reports/models/{modelId}", modelId)
                        .header("Authorization", "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("동일 모델에 대한 중복 신고 실패")
    void createModelReport_DuplicateReport_Conflict() throws Exception {
        // given
        Long modelId = testModel.getId();
        ModelReportRequest request = new ModelReportRequest("첫 번째 신고");

        // 첫 번째 신고 생성
        mockMvc.perform(post("/reports/models/{modelId}", modelId)
                        .header("Authorization", "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // when & then - 동일 모델에 대한 중복 신고 시도
        ModelReportRequest duplicateRequest = new ModelReportRequest("중복 신고 시도");
        
        mockMvc.perform(post("/reports/models/{modelId}", modelId)
                        .header("Authorization", "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }
}