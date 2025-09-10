package com.example.nomodel.report.application.controller;

import com.example.nomodel._core.config.RestDocsConfiguration;
import com.example.nomodel._core.config.SecurityConfig;
import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel._core.security.CustomUserDetailsService;
import com.example.nomodel._core.security.jwt.JWTTokenProvider;
import com.example.nomodel.report.application.dto.request.ModelReportRequest;
import com.example.nomodel.report.application.dto.response.ModelReportResponse;
import com.example.nomodel.report.application.service.ModelReportService;
import com.example.nomodel.report.domain.model.ReportStatus;
import com.example.nomodel.report.domain.model.TargetType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ModelReportController.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Import({RestDocsConfiguration.class, SecurityConfig.class})
@DisplayName("ModelReportController 단위 테스트")
class ModelReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ModelReportService modelReportService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JWTTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("모델 신고 성공")
    @WithMockUser
    void createModelReport_Success() throws Exception {
        // given
        Long modelId = 1L;
        String reasonDetail = "부적절한 콘텐츠가 포함되어 있습니다.";
        ModelReportRequest request = new ModelReportRequest(reasonDetail);
        
        ModelReportResponse response = ModelReportResponse.builder()
                .reportId(1L)
                .targetType(TargetType.MODEL.getValue())
                .targetId(modelId)
                .reporterId(1L)
                .reasonDetail(reasonDetail)
                .reportStatus(ReportStatus.PENDING.getValue())
                .reportStatusDescription(ReportStatus.PENDING.getDescription())
                .adminNote(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(modelReportService.createModelReport(eq(modelId), eq(1L), any(ModelReportRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/reports/models/{modelId}", modelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(createCustomUserDetails(1L))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.reportId").value(1))
                .andExpect(jsonPath("$.response.targetType").value("MODEL"))
                .andExpect(jsonPath("$.response.targetId").value(modelId))
                .andExpect(jsonPath("$.response.reasonDetail").value(reasonDetail))
                .andExpect(jsonPath("$.response.reportStatus").value("PENDING"))
                .andDo(document("model-report-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("modelId").description("신고할 모델 ID")
                        ),
                        requestFields(
                                fieldWithPath("reasonDetail").type(JsonFieldType.STRING)
                                        .description("신고 상세 사유 (최대 1000자)")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT)
                                        .description("신고 응답 데이터"),
                                fieldWithPath("response.reportId").type(JsonFieldType.NUMBER)
                                        .description("생성된 신고 ID"),
                                fieldWithPath("response.targetType").type(JsonFieldType.STRING)
                                        .description("신고 대상 타입 (MODEL)"),
                                fieldWithPath("response.targetId").type(JsonFieldType.NUMBER)
                                        .description("신고 대상 ID (모델 ID)"),
                                fieldWithPath("response.reporterId").type(JsonFieldType.NUMBER)
                                        .description("신고자 ID"),
                                fieldWithPath("response.reasonDetail").type(JsonFieldType.STRING)
                                        .description("신고 상세 사유"),
                                fieldWithPath("response.reportStatus").type(JsonFieldType.STRING)
                                        .description("신고 상태 (PENDING, UNDER_REVIEW, REJECTED, RESOLVED)"),
                                fieldWithPath("response.reportStatusDescription").type(JsonFieldType.STRING)
                                        .description("신고 상태 설명"),
                                fieldWithPath("response.adminNote").type(JsonFieldType.NULL)
                                        .description("관리자 메모").optional(),
                                fieldWithPath("response.createdAt").type(JsonFieldType.STRING)
                                        .description("신고 생성일시"),
                                fieldWithPath("response.updatedAt").type(JsonFieldType.STRING)
                                        .description("신고 수정일시"),
                                fieldWithPath("error").type(JsonFieldType.NULL)
                                        .description("에러 정보 (성공시 null)").optional()
                        )
                ));

        then(modelReportService).should().createModelReport(eq(modelId), eq(1L), any(ModelReportRequest.class));
    }

    @Test
    @DisplayName("모델 신고 실패 - 유효성 검증 오류")
    @WithMockUser
    void createModelReport_ValidationError() throws Exception {
        // given - 빈 신고 사유
        Long modelId = 1L;
        ModelReportRequest request = new ModelReportRequest("");

        // when & then
        mockMvc.perform(post("/reports/models/{modelId}", modelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(createCustomUserDetails(1L))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.status").value(400))
                .andExpect(jsonPath("$.error.errorCode").exists())
                .andExpect(jsonPath("$.error.message").exists())
                .andExpect(jsonPath("$.error.timestamp").exists());

        then(modelReportService).should(never()).createModelReport(any(), any(), any());
    }

    @Test
    @DisplayName("모델 신고 실패 - 중복 신고")
    @WithMockUser
    void createModelReport_DuplicateReport() throws Exception {
        // given
        Long modelId = 1L;
        ModelReportRequest request = new ModelReportRequest("부적절한 콘텐츠입니다.");

        given(modelReportService.createModelReport(eq(modelId), eq(1L), any(ModelReportRequest.class)))
                .willThrow(new ApplicationException(ErrorCode.DUPLICATE_REPORT));

        // when & then
        mockMvc.perform(post("/reports/models/{modelId}", modelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(createCustomUserDetails(1L))))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.status").value(409))
                .andExpect(jsonPath("$.error.errorCode").value("RP002"))
                .andExpect(jsonPath("$.error.message").value("Report already exists"))
                .andExpect(jsonPath("$.error.timestamp").exists());
    }

    @Test
    @DisplayName("내 모델 신고 목록 조회 성공")
    @WithMockUser
    void getMyModelReports_Success() throws Exception {
        // given
        List<ModelReportResponse> reports = Arrays.asList(
                ModelReportResponse.builder()
                        .reportId(1L)
                        .targetType(TargetType.MODEL.getValue())
                        .targetId(100L)
                        .reporterId(1L)
                        .reasonDetail("부적절한 콘텐츠입니다.")
                        .reportStatus(ReportStatus.PENDING.getValue())
                        .reportStatusDescription(ReportStatus.PENDING.getDescription())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),
                ModelReportResponse.builder()
                        .reportId(2L)
                        .targetType(TargetType.MODEL.getValue())
                        .targetId(200L)
                        .reporterId(1L)
                        .reasonDetail("저작권 침해 의심됩니다.")
                        .reportStatus(ReportStatus.RESOLVED.getValue())
                        .reportStatusDescription(ReportStatus.RESOLVED.getDescription())
                        .adminNote("처리 완료되었습니다.")
                        .createdAt(LocalDateTime.now().minusDays(1))
                        .updatedAt(LocalDateTime.now())
                        .build()
        );

        given(modelReportService.getUserModelReports(eq(1L))).willReturn(reports);

        // when & then
        mockMvc.perform(get("/reports/my/models")
                        .with(user(createCustomUserDetails(1L))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").isArray())
                .andExpect(jsonPath("$.response.length()").value(2))
                .andExpect(jsonPath("$.response[0].reportId").value(1))
                .andExpect(jsonPath("$.response[0].reportStatus").value("PENDING"))
                .andExpect(jsonPath("$.response[1].reportId").value(2))
                .andExpect(jsonPath("$.response[1].reportStatus").value("RESOLVED"))
                .andDo(document("model-report-my-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("response").type(JsonFieldType.ARRAY)
                                        .description("내 신고 목록"),
                                fieldWithPath("response[].reportId").type(JsonFieldType.NUMBER)
                                        .description("신고 ID"),
                                fieldWithPath("response[].targetType").type(JsonFieldType.STRING)
                                        .description("신고 대상 타입"),
                                fieldWithPath("response[].targetId").type(JsonFieldType.NUMBER)
                                        .description("신고 대상 ID (모델 ID)"),
                                fieldWithPath("response[].reporterId").type(JsonFieldType.NUMBER)
                                        .description("신고자 ID"),
                                fieldWithPath("response[].reasonDetail").type(JsonFieldType.STRING)
                                        .description("신고 상세 사유"),
                                fieldWithPath("response[].reportStatus").type(JsonFieldType.STRING)
                                        .description("신고 상태"),
                                fieldWithPath("response[].reportStatusDescription").type(JsonFieldType.STRING)
                                        .description("신고 상태 설명"),
                                fieldWithPath("response[].adminNote").type(JsonFieldType.VARIES)
                                        .description("관리자 메모").optional(),
                                fieldWithPath("response[].createdAt").type(JsonFieldType.STRING)
                                        .description("신고 생성일시"),
                                fieldWithPath("response[].updatedAt").type(JsonFieldType.STRING)
                                        .description("신고 수정일시"),
                                fieldWithPath("error").type(JsonFieldType.NULL)
                                        .description("에러 정보 (성공시 null)").optional()
                        )
                ));

        then(modelReportService).should().getUserModelReports(eq(1L));
    }

    @Test
    @DisplayName("특정 신고 상세 조회 성공")
    @WithMockUser
    void getModelReport_Success() throws Exception {
        // given
        Long reportId = 1L;
        ModelReportResponse report = ModelReportResponse.builder()
                .reportId(reportId)
                .targetType(TargetType.MODEL.getValue())
                .targetId(100L)
                .reporterId(1L)
                .reasonDetail("부적절한 콘텐츠가 포함되어 있습니다.")
                .reportStatus(ReportStatus.UNDER_REVIEW.getValue())
                .reportStatusDescription(ReportStatus.UNDER_REVIEW.getDescription())
                .adminNote("검토 진행 중입니다.")
                .createdAt(LocalDateTime.now().minusHours(2))
                .updatedAt(LocalDateTime.now().minusMinutes(30))
                .build();

        given(modelReportService.getModelReport(eq(reportId), eq(1L))).willReturn(report);

        // when & then
        mockMvc.perform(get("/reports/{reportId}", reportId)
                        .with(user(createCustomUserDetails(1L))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.reportId").value(reportId))
                .andExpect(jsonPath("$.response.reportStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.response.adminNote").value("검토 진행 중입니다."))
                .andDo(document("model-report-detail",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("reportId").description("조회할 신고 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT)
                                        .description("신고 상세 정보"),
                                fieldWithPath("response.reportId").type(JsonFieldType.NUMBER)
                                        .description("신고 ID"),
                                fieldWithPath("response.targetType").type(JsonFieldType.STRING)
                                        .description("신고 대상 타입"),
                                fieldWithPath("response.targetId").type(JsonFieldType.NUMBER)
                                        .description("신고 대상 ID (모델 ID)"),
                                fieldWithPath("response.reporterId").type(JsonFieldType.NUMBER)
                                        .description("신고자 ID"),
                                fieldWithPath("response.reasonDetail").type(JsonFieldType.STRING)
                                        .description("신고 상세 사유"),
                                fieldWithPath("response.reportStatus").type(JsonFieldType.STRING)
                                        .description("신고 상태"),
                                fieldWithPath("response.reportStatusDescription").type(JsonFieldType.STRING)
                                        .description("신고 상태 설명"),
                                fieldWithPath("response.adminNote").type(JsonFieldType.STRING)
                                        .description("관리자 메모").optional(),
                                fieldWithPath("response.createdAt").type(JsonFieldType.STRING)
                                        .description("신고 생성일시"),
                                fieldWithPath("response.updatedAt").type(JsonFieldType.STRING)
                                        .description("신고 수정일시"),
                                fieldWithPath("error").type(JsonFieldType.NULL)
                                        .description("에러 정보 (성공시 null)").optional()
                        )
                ));

        then(modelReportService).should().getModelReport(eq(reportId), eq(1L));
    }

    @Test
    @DisplayName("특정 신고 상세 조회 실패 - 권한 없음")
    @WithMockUser
    void getModelReport_AccessDenied() throws Exception {
        // given
        Long reportId = 1L;
        given(modelReportService.getModelReport(eq(reportId), eq(1L)))
                .willThrow(new ApplicationException(ErrorCode.ACCESS_DENIED));

        // when & then
        mockMvc.perform(get("/reports/{reportId}", reportId)
                        .with(user(createCustomUserDetails(1L))))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.status").value(403))
                .andExpect(jsonPath("$.error.errorCode").value("AD001"))
                .andExpect(jsonPath("$.error.message").value("Access denied"))
                .andExpect(jsonPath("$.error.timestamp").exists());
    }

    @Test
    @DisplayName("특정 신고 상세 조회 실패 - 신고 없음")
    @WithMockUser
    void getModelReport_NotFound() throws Exception {
        // given
        Long reportId = 999L;
        given(modelReportService.getModelReport(eq(reportId), eq(1L)))
                .willThrow(new ApplicationException(ErrorCode.REPORT_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/reports/{reportId}", reportId)
                        .with(user(createCustomUserDetails(1L))))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.status").value(404))
                .andExpect(jsonPath("$.error.errorCode").value("RNF001"))
                .andExpect(jsonPath("$.error.message").value("Report not found"))
                .andExpect(jsonPath("$.error.timestamp").exists());
    }

    /**
     * 테스트용 CustomUserDetails 생성
     */
    private CustomUserDetails createCustomUserDetails(Long memberId) {
        return new CustomUserDetails(
                memberId, 
                "test@example.com", 
                "password", 
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}