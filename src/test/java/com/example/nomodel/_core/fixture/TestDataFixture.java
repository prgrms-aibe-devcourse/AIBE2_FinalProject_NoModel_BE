package com.example.nomodel._core.fixture;

import com.example.nomodel._core.security.jwt.JWTTokenProvider;
import com.example.nomodel.member.application.dto.response.AuthTokenDTO;
import com.example.nomodel.member.domain.model.*;
import com.example.nomodel.model.domain.model.AIModel;
import com.example.nomodel.model.domain.model.ModelMetadata;
import com.example.nomodel.model.domain.model.OwnType;
import com.example.nomodel.model.domain.model.SamplerType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Collections;

/**
 * 테스트용 더미 데이터 생성 유틸리티
 */
public class TestDataFixture {

    // Member 생성 헬퍼 메서드들
    
    /**
     * 기본 사용자 Member 생성
     */
    public static Member createDefaultMember(PasswordEncoder passwordEncoder) {
        return createMember("testUser", "test@example.com", "testPassword", Role.USER, Status.ACTIVE, passwordEncoder);
    }
    
    /**
     * 관리자 Member 생성
     */
    public static Member createAdminMember(PasswordEncoder passwordEncoder) {
        return createMember("adminUser", "admin@example.com", "adminPassword", Role.ADMIN, Status.ACTIVE, passwordEncoder);
    }
    
    /**
     * 정지된 Member 생성
     */
    public static Member createSuspendedMember(PasswordEncoder passwordEncoder) {
        return createMember("suspendedUser", "suspended@example.com", "suspendedPassword", Role.USER, Status.SUSPENDED, passwordEncoder);
    }
    
    /**
     * 커스텀 Member 생성
     */
    public static Member createMember(String username, String email, String password, Role role, Status status, PasswordEncoder passwordEncoder) {
        return Member.builder()
                .username(username)
                .email(Email.of(email))
                .password(Password.encode(password, passwordEncoder))
                .role(role)
                .status(status)
                .build();
    }

    // AIModel 생성 헬퍼 메서드들
    
    /**
     * 기본 AIModel 생성
     */
    public static AIModel createDefaultAIModel(Long ownerId) {
        return createAIModel("Test Model", ownerId, "Test model prompt", BigDecimal.valueOf(10.0), true, OwnType.USER);
    }
    
    /**
     * 비공개 AIModel 생성
     */
    public static AIModel createPrivateAIModel(Long ownerId) {
        return createAIModel("Private Model", ownerId, "Private model prompt", BigDecimal.valueOf(20.0), false, OwnType.USER);
    }
    
    /**
     * 관리자 AIModel 생성
     */
    public static AIModel createAdminAIModel() {
        return createAIModel("Admin Model", null, "Admin model prompt", BigDecimal.ZERO, true, OwnType.ADMIN);
    }
    
    /**
     * 커스텀 AIModel 생성
     */
    public static AIModel createAIModel(String modelName, Long ownerId, String prompt, BigDecimal price, boolean isPublic, OwnType ownType) {
        ModelMetadata modelMetadata = createDefaultModelMetadata(prompt);
        
        return AIModel.builder()
                .modelName(modelName)
                .modelMetadata(modelMetadata)
                .ownType(ownType)
                .ownerId(ownerId)
                .price(price)
                .isPublic(isPublic)
                .build();
    }

    // ModelMetadata 생성 헬퍼 메서드들
    
    /**
     * 기본 ModelMetadata 생성
     */
    public static ModelMetadata createDefaultModelMetadata() {
        return createDefaultModelMetadata("Test model prompt");
    }
    
    /**
     * 기본 ModelMetadata 생성 (커스텀 프롬프트)
     */
    public static ModelMetadata createDefaultModelMetadata(String prompt) {
        return createModelMetadata(1234L, prompt, "Test negative prompt", 512, 512, 20, SamplerType.EULER_A, 1, 1);
    }
    
    /**
     * 고해상도 ModelMetadata 생성
     */
    public static ModelMetadata createHighResModelMetadata() {
        return createModelMetadata(5678L, "High resolution prompt", "High res negative", 1920, 1080, 30, SamplerType.DPM_PLUS_PLUS_2M_KARRAS, 1, 1);
    }
    
    /**
     * 배치 처리용 ModelMetadata 생성
     */
    public static ModelMetadata createBatchModelMetadata() {
        return createModelMetadata(9999L, "Batch processing prompt", "Batch negative", 768, 768, 25, SamplerType.LMS_KARRAS, 4, 2);
    }
    
    /**
     * 커스텀 ModelMetadata 생성
     */
    public static ModelMetadata createModelMetadata(Long seed, String prompt, String negativePrompt, 
                                                   Integer width, Integer height, Integer steps, 
                                                   SamplerType samplerIndex, Integer nIter, Integer batchSize) {
        return ModelMetadata.builder()
                .seed(seed)
                .prompt(prompt)
                .negativePrompt(negativePrompt)
                .width(width)
                .height(height)
                .steps(steps)
                .samplerIndex(samplerIndex)
                .nIter(nIter)
                .batchSize(batchSize)
                .build();
    }

    // JWT 토큰 생성 헬퍼 메서드들
    
    /**
     * Member용 JWT 토큰 생성
     */
    public static String createJwtToken(Member member, JWTTokenProvider jwtTokenProvider) {
        AuthTokenDTO authToken = jwtTokenProvider.generateToken(
                member.getEmail().getValue(),
                member.getId(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()))
        );
        return authToken.accessToken();
    }
    
    /**
     * 사용자 권한 JWT 토큰 생성
     */
    public static String createUserJwtToken(String email, Long memberId, JWTTokenProvider jwtTokenProvider) {
        AuthTokenDTO authToken = jwtTokenProvider.generateToken(
                email,
                memberId,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        return authToken.accessToken();
    }
    
    /**
     * 관리자 권한 JWT 토큰 생성
     */
    public static String createAdminJwtToken(String email, Long memberId, JWTTokenProvider jwtTokenProvider) {
        AuthTokenDTO authToken = jwtTokenProvider.generateToken(
                email,
                memberId,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        return authToken.accessToken();
    }

    // 편의 메서드들
    
    /**
     * 테스트용 이메일 생성 (순번 기반)
     */
    public static String createTestEmail(int index) {
        return String.format("test%d@example.com", index);
    }
    
    /**
     * 테스트용 사용자명 생성 (순번 기반)
     */
    public static String createTestUsername(int index) {
        return String.format("testUser%d", index);
    }
    
    /**
     * 테스트용 모델명 생성 (순번 기반)
     */
    public static String createTestModelName(int index) {
        return String.format("Test Model %d", index);
    }
}