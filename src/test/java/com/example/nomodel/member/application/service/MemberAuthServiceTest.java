package com.example.nomodel.member.application.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel._core.security.jwt.JWTTokenProvider;
import com.example.nomodel.member.application.dto.request.LoginRequestDto;
import com.example.nomodel.member.application.dto.request.SignUpRequestDto;
import com.example.nomodel.member.application.dto.response.AuthTokenDTO;
import com.example.nomodel.member.domain.model.*;
import com.example.nomodel.member.domain.repository.FirstLoginRedisRepository;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.member.domain.repository.RefreshTokenRedisRepository;
import com.example.nomodel.member.domain.service.LoginSecurityDomainService;
import com.example.nomodel.member.domain.service.MemberDomainService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberAuthService 단위 테스트")
class MemberAuthServiceTest {

    @Mock
    private MemberJpaRepository memberJpaRepository;
    
    @Mock
    private MemberDomainService memberDomainService;
    
    @Mock
    private LoginSecurityDomainService loginSecurityDomainService;
    
    @Mock
    private FirstLoginRedisRepository firstLoginRedisRepository;
    
    @Mock
    private RefreshTokenRedisRepository refreshTokenRedisRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JWTTokenProvider jwtTokenProvider;
    
    @Mock
    private AuthenticationManagerBuilder authenticationManagerBuilder;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private Authentication authentication;
    
    @Mock
    private Member member;

    @InjectMocks
    private MemberAuthService memberAuthService;

    @Test
    @DisplayName("회원가입 성공")
    void signUp_Success() {
        // given
        SignUpRequestDto requestDto = new SignUpRequestDto("testUser", "test@example.com", "password123");
        Email email = Email.of(requestDto.email());
        Password password = Password.encode(requestDto.password(), passwordEncoder);
        Member savedMember = Member.createMember(requestDto.username(), email, password);
        savedMember.setId(1L);

        given(passwordEncoder.encode(any())).willReturn("encoded-password");
        given(memberJpaRepository.save(any(Member.class))).willReturn(savedMember);

        // when & then
        assertThatNoException().isThrownBy(() -> memberAuthService.signUp(requestDto));
        
        then(memberDomainService).should().validateEmailUniqueness(any(Email.class));
        then(memberJpaRepository).should().save(any(Member.class));
        then(firstLoginRedisRepository).should().setFirstLoginStatus(1L, true);
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() {
        // given
        LoginRequestDto requestDto = new LoginRequestDto("test@example.com", "password123");
        
        AuthTokenDTO expectedTokenDto = new AuthTokenDTO("Bearer", "access-token", 3600000L, "refresh-token", 604800000L);
        
        given(memberJpaRepository.findByEmail(any(Email.class))).willReturn(Optional.of(member));
        given(member.getId()).willReturn(1L);
        given(authenticationManagerBuilder.getObject()).willReturn(authenticationManager);
        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(authentication.getAuthorities()).willReturn(List.of());
        given(loginSecurityDomainService.getCurrentClientIp()).willReturn("192.168.1.1");
        given(jwtTokenProvider.generateToken(anyString(), any(Long.class), any())).willReturn(expectedTokenDto);

        // when
        AuthTokenDTO result = memberAuthService.login(requestDto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.grantType()).isEqualTo("Bearer");
        
        then(loginSecurityDomainService).should().validateCurrentIpNotBlocked();
        then(memberJpaRepository).should().findByEmail(any(Email.class));
        then(jwtTokenProvider).should().generateToken(anyString(), eq(1L), any());
        then(refreshTokenRedisRepository).should().save(any(RefreshToken.class));
    }


    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 회원")
    void login_MemberNotFound() {
        // given
        LoginRequestDto requestDto = new LoginRequestDto("nonexistent@example.com", "password123");
        
        given(memberJpaRepository.findByEmail(any(Email.class))).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberAuthService.login(requestDto))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
                
        then(loginSecurityDomainService).should().validateCurrentIpNotBlocked();
    }

    @Test
    @DisplayName("로그인 실패 - IP 차단")
    void login_IpBlocked() {
        // given
        LoginRequestDto requestDto = new LoginRequestDto("test@example.com", "password123");
        
        willThrow(new ApplicationException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS))
                .given(loginSecurityDomainService).validateCurrentIpNotBlocked();

        // when & then
        assertThatThrownBy(() -> memberAuthService.login(requestDto))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
                
        then(loginSecurityDomainService).should().validateCurrentIpNotBlocked();
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void refreshToken_Success() {
        // given
        String refreshTokenValue = "refresh-token-value";
        RefreshToken storedToken = RefreshToken.builder()
                .id("123")
                .refreshToken(refreshTokenValue)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
        
        AuthTokenDTO expectedTokenDto = new AuthTokenDTO("Bearer", "new-access-token", 3600000L, "new-refresh-token", 604800000L);
        
        given(jwtTokenProvider.resolveToken(request)).willReturn(refreshTokenValue);
        given(jwtTokenProvider.validateToken(refreshTokenValue)).willReturn(true);
        given(jwtTokenProvider.isRefreshToken(refreshTokenValue)).willReturn(true);
        given(refreshTokenRedisRepository.findByRefreshToken(refreshTokenValue)).willReturn(storedToken);
        given(jwtTokenProvider.generateToken(anyString(), any(Long.class), any())).willReturn(expectedTokenDto);

        // when
        AuthTokenDTO result = memberAuthService.refreshToken(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.grantType()).isEqualTo("Bearer");
        assertThat(result.accessToken()).isEqualTo("new-access-token");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 토큰 없음")
    void refreshToken_TokenNotFound() {
        // given
        given(jwtTokenProvider.resolveToken(request)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> memberAuthService.refreshToken(request))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_NOT_FOUND);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 토큰")
    void refreshToken_InvalidToken() {
        // given
        String refreshTokenValue = "invalid-refresh-token";
        
        given(jwtTokenProvider.resolveToken(request)).willReturn(refreshTokenValue);
        given(jwtTokenProvider.validateToken(refreshTokenValue)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> memberAuthService.refreshToken(request))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_Success() {
        // given
        String refreshTokenValue = "refresh-token-value";
        
        given(jwtTokenProvider.resolveToken(request)).willReturn(refreshTokenValue);
        given(jwtTokenProvider.validateToken(refreshTokenValue)).willReturn(true);

        RefreshToken storedToken = RefreshToken.builder()
                .id("123")
                .refreshToken(refreshTokenValue)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
        
        given(refreshTokenRedisRepository.findByRefreshToken(refreshTokenValue)).willReturn(storedToken);

        // when & then
        assertThatNoException().isThrownBy(() -> memberAuthService.logout(request));
        
        then(refreshTokenRedisRepository).should().findByRefreshToken(refreshTokenValue);
        then(refreshTokenRedisRepository).should().deleteById("123");
    }

    @Test
    @DisplayName("로그아웃 실패 - 토큰 없음")
    void logout_TokenNotFound() {
        // given
        given(jwtTokenProvider.resolveToken(request)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> memberAuthService.logout(request))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_NOT_FOUND);
    }
}