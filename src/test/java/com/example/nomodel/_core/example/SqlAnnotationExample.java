package com.example.nomodel._core.example;

import com.example.nomodel._core.config.RestDocsConfiguration;
import com.example.nomodel._core.config.TestOAuth2Config;
import com.example.nomodel._core.security.jwt.JWTTokenProvider;
import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.model.domain.repository.AIModelJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @Sql 어노테이션 사용 예제
 * 다양한 방식으로 테스트 데이터를 로드하고 활용하는 방법 데모
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Transactional
@Import({TestOAuth2Config.class, RestDocsConfiguration.class})
@DisplayName("@Sql 어노테이션 사용 예제")
public class SqlAnnotationExample {

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

    // ========== 방법 1: 클래스 레벨에서 전체 테스트 데이터 로드 ==========
    // 모든 테스트 메서드에서 공통으로 사용할 데이터
    // @Sql("/sql/comprehensive-test-data.sql")
    // 위 어노테이션을 클래스 위에 추가하면 모든 테스트에서 데이터 사용 가능

    // ========== 방법 2: 메서드별 개별 데이터 로드 ==========
    @Test
    @Sql("/sql/comprehensive-test-data.sql") // 이 테스트만을 위한 데이터 로드
    @DisplayName("전체 테스트 데이터로 회원 및 모델 조회 테스트")
    void testWithFullDataset() {
        // 로드된 데이터 확인
        var members = memberRepository.findAll();
        var models = aiModelRepository.findAll();
        
        // 예상 데이터 개수 검증
        assert members.size() == 8; // 8명의 회원
        assert models.size() == 6;  // 6개의 모델
        
        // 특정 사용자 데이터 확인
        var activeCreator = memberRepository.findById(5L).orElseThrow();
        assert "activeCreator".equals(activeCreator.getUsername());
        
        // 해당 사용자의 모델 확인
        var creatorModels = aiModelRepository.findByOwnerId(5L);
        assert creatorModels.size() == 3; // activeCreator가 만든 모델 3개
    }

    // ========== 방법 3: 여러 SQL 파일 조합 ==========
    @Test
    @SqlGroup({
        @Sql("/sql/basic-members.sql"),     // 기본 회원 데이터만
        @Sql("/sql/sample-models.sql")      // 샘플 모델 데이터만
    })
    @DisplayName("여러 SQL 파일을 조합한 테스트")
    void testWithMultipleSqlFiles() {
        // 필요한 데이터만 선별적으로 로드하여 테스트
        // 더 가벼운 테스트 환경 구성 가능
    }

    // ========== 방법 4: 실행 시점 제어 ==========
    @Test
    @Sql(scripts = "/sql/comprehensive-test-data.sql", 
         executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/cleanup-test-data.sql", 
         executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @DisplayName("테스트 전후 SQL 실행 제어")
    void testWithBeforeAndAfterSql() {
        // 테스트 전: comprehensive-test-data.sql 실행
        // 테스트 후: cleanup-test-data.sql 실행
        
        var members = memberRepository.findAll();
        assert !members.isEmpty();
    }

    // ========== 방법 5: JWT 토큰과 함께 활용 ==========
    @Test
    @Sql("/sql/comprehensive-test-data.sql")
    @DisplayName("로드된 테스트 데이터로 JWT 토큰 생성 및 API 테스트")
    void testApiWithPreloadedData() throws Exception {
        // 미리 로드된 회원 데이터로 JWT 토큰 생성
        var testMember = memberRepository.findById(1L).orElseThrow(); // normalUser
        
        // JWT 토큰 생성 (실제 DB 데이터 활용)
        String token = jwtTokenProvider.generateToken(
            testMember.getEmail().getValue(),
            testMember.getId(),
            java.util.Collections.singletonList(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + testMember.getRole().name())
            )
        ).accessToken();
        
        // API 테스트 - 내 모델 목록 조회 (실제 DB 데이터 사용)
        mockMvc.perform(get("/api/models/my")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ========== 방법 6: 특정 시나리오별 데이터 ==========
    @Test
    @Sql("/sql/premium-user-scenario.sql") // 프리미엄 사용자 시나리오용 데이터
    @DisplayName("프리미엄 사용자 시나리오 테스트")
    void testPremiumUserScenario() {
        // 프리미엄 구독, 포인트, 쿠폰 등이 설정된 특별한 시나리오 테스트
    }

    @Test
    @Sql("/sql/admin-operations-scenario.sql") // 관리자 기능 테스트용 데이터
    @DisplayName("관리자 기능 테스트")
    void testAdminOperations() {
        // 신고, 승인, 정지 등 관리자 권한이 필요한 기능 테스트
    }

    // ========== 방법 7: 테스트 격리를 위한 롤백 확인 ==========
    @Test
    @Sql("/sql/comprehensive-test-data.sql")
    @DisplayName("첫 번째 테스트 - 데이터 수정")
    void firstTestModifyingData() {
        // 데이터 수정
        var member = memberRepository.findById(1L).orElseThrow();
        member.updateUsername("modifiedUser");
        memberRepository.save(member);
        
        assert "modifiedUser".equals(member.getUsername());
    }

    @Test
    @Sql("/sql/comprehensive-test-data.sql")
    @DisplayName("두 번째 테스트 - 원본 데이터 확인")
    void secondTestCheckingOriginalData() {
        // @Transactional에 의해 이전 테스트의 변경사항은 롤백됨
        var member = memberRepository.findById(1L).orElseThrow();
        assert "normalUser".equals(member.getUsername()); // 원본 데이터 유지
    }
}