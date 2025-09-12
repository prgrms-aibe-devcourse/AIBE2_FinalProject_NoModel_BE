package com.example.nomodel._core.security.jwt;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel.member.application.dto.response.AuthTokenDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWTTokenProvider 단위 테스트")
class JWTTokenProviderTest {

    private JWTTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Authentication authentication;

    private final String secretKey = Base64.getEncoder().encodeToString("my-very-long-secret-key-for-jwt-token-generation-test".getBytes(StandardCharsets.UTF_8));
    private final long accessTokenLifetime = 3600000L; // 1시간
    private final long refreshTokenLifetime = 604800000L; // 7일

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JWTTokenProvider(secretKey, accessTokenLifetime / 1000, refreshTokenLifetime / 1000);
    }

    @Test
    @DisplayName("토큰 생성 - Authentication 객체로")
    void generateToken_WithAuthentication_Success() {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(
            1L, 
            "test@example.com", 
            "password", 
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        
        given(authentication.getName()).willReturn("1"); // memberId가 username
        given(authentication.getAuthorities()).willReturn((Collection)List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // when
        AuthTokenDTO result = jwtTokenProvider.generateToken(authentication);

        // then
        assertThat(result).isNotNull();
        assertThat(result.grantType()).isEqualTo("Bearer");
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.accessTokenValidTime()).isEqualTo(accessTokenLifetime);
        assertThat(result.refreshTokenValidTime()).isEqualTo(refreshTokenLifetime);
    }

    @Test
    @DisplayName("토큰 생성 - email과 memberId로")
    void generateToken_WithEmailAndMemberId_Success() {
        // given
        String email = "test@example.com";
        Long memberId = 1L;
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        // when
        AuthTokenDTO result = jwtTokenProvider.generateToken(email, memberId, authorities);

        // then
        assertThat(result).isNotNull();
        assertThat(result.grantType()).isEqualTo("Bearer");
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        
        // 토큰 파싱하여 내용 검증
        Claims claims = jwtTokenProvider.parseClaims(result.accessToken());
        assertThat(claims.getSubject()).isEqualTo(String.valueOf(memberId));
        assertThat(claims.get("auth")).isEqualTo("ROLE_USER");
        assertThat(claims.get("type")).isEqualTo("access");
        assertThat(claims.get("isFirstLogin")).isEqualTo(false); // 기본값 false
    }

    @Test
    @DisplayName("토큰 생성 - 최초 로그인 여부 포함")
    void generateToken_WithIsFirstLogin_Success() {
        // given
        String email = "test@example.com";
        Long memberId = 1L;
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        Boolean isFirstLogin = true;

        // when
        AuthTokenDTO result = jwtTokenProvider.generateToken(email, memberId, authorities, isFirstLogin);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isNotBlank();
        
        // 토큰 파싱하여 isFirstLogin 클레임 검증
        Claims claims = jwtTokenProvider.parseClaims(result.accessToken());
        assertThat(claims.getSubject()).isEqualTo(String.valueOf(memberId));
        assertThat(claims.get("auth")).isEqualTo("ROLE_USER");
        assertThat(claims.get("type")).isEqualTo("access");
        assertThat(claims.get("isFirstLogin")).isEqualTo(true);
    }

    @Test
    @DisplayName("토큰 생성 - 최초 로그인 false")
    void generateToken_WithIsFirstLoginFalse_Success() {
        // given
        String email = "test@example.com";
        Long memberId = 1L;
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        Boolean isFirstLogin = false;

        // when
        AuthTokenDTO result = jwtTokenProvider.generateToken(email, memberId, authorities, isFirstLogin);

        // then
        Claims claims = jwtTokenProvider.parseClaims(result.accessToken());
        assertThat(claims.get("isFirstLogin")).isEqualTo(false);
    }

    @Test
    @DisplayName("유효한 토큰 검증")
    void validateToken_ValidToken_ReturnsTrue() {
        // given
        String email = "test@example.com";
        Long memberId = 1L;
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        AuthTokenDTO tokenDto = jwtTokenProvider.generateToken(email, memberId, authorities);

        // when
        boolean isValid = jwtTokenProvider.validateToken(tokenDto.accessToken());

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("유효하지 않은 토큰 검증")
    void validateToken_InvalidToken_ReturnsFalse() {
        // given
        String invalidToken = "invalid.jwt.token";

        // when
        boolean result = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰 검증")
    void validateToken_ExpiredToken_ReturnsFalse() {
        // given - 이미 만료된 토큰 생성
        String expiredToken = Jwts.builder()
                .setSubject("1")
                .claim("auth", "ROLE_USER")
                .claim("type", "access")
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2시간 전
                .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // 1시간 전 만료
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKey)))
                .compact();

        // when
        boolean result = jwtTokenProvider.validateToken(expiredToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("토큰에서 Authentication 추출")
    void getAuthentication_ValidToken_ReturnsAuthentication() {
        // given
        String email = "test@example.com";
        Long memberId = 1L;
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        AuthTokenDTO tokenDto = jwtTokenProvider.generateToken(email, memberId, authorities);

        // when
        Authentication result = jwtTokenProvider.getAuthentication(tokenDto.accessToken());

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPrincipal()).isInstanceOf(CustomUserDetails.class);
        
        CustomUserDetails userDetails = (CustomUserDetails) result.getPrincipal();
        assertThat(userDetails.getMemberId()).isEqualTo(memberId);
        assertThat(userDetails.getUsername()).isEqualTo(String.valueOf(memberId));
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities()).extracting("authority").contains("ROLE_USER");
    }

    @Test
    @DisplayName("권한 정보가 없는 토큰에서 Authentication 추출 실패")
    void getAuthentication_TokenWithoutAuthorities_ThrowsException() {
        // given - 권한 정보가 없는 토큰
        String tokenWithoutAuth = Jwts.builder()
                .setSubject("1")
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenLifetime))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKey)))
                .compact();

        // when & then
        assertThatThrownBy(() -> jwtTokenProvider.getAuthentication(tokenWithoutAuth))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_AUTHORITIES_IN_TOKEN);
    }

    @Test
    @DisplayName("리프레시 토큰 타입 확인")
    void isRefreshToken_RefreshToken_ReturnsTrue() {
        // given
        String email = "test@example.com";
        Long memberId = 1L;
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        AuthTokenDTO tokenDto = jwtTokenProvider.generateToken(email, memberId, authorities);

        // when
        boolean isRefreshToken = jwtTokenProvider.isRefreshToken(tokenDto.refreshToken());

        // then
        assertThat(isRefreshToken).isTrue();
    }

    @Test
    @DisplayName("액세스 토큰 타입 확인")
    void isRefreshToken_AccessToken_ReturnsFalse() {
        // given
        String email = "test@example.com";
        Long memberId = 1L;
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        AuthTokenDTO tokenDto = jwtTokenProvider.generateToken(email, memberId, authorities);

        // when
        boolean isRefreshToken = jwtTokenProvider.isRefreshToken(tokenDto.accessToken());

        // then
        assertThat(isRefreshToken).isFalse();
    }

    @Test
    @DisplayName("HTTP 요청에서 토큰 추출 성공")
    void resolveToken_ValidAuthorizationHeader_ReturnsToken() {
        // given
        String token = "jwt-token-value";
        given(request.getHeader("Authorization")).willReturn("Bearer " + token);

        // when
        String result = jwtTokenProvider.resolveToken(request);

        // then
        assertThat(result).isEqualTo(token);
    }

    @Test
    @DisplayName("HTTP 요청에서 토큰 추출 실패 - Authorization 헤더 없음")
    void resolveToken_NoAuthorizationHeader_ReturnsNull() {
        // given
        given(request.getHeader("Authorization")).willReturn(null);

        // when
        String result = jwtTokenProvider.resolveToken(request);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("HTTP 요청에서 토큰 추출 실패 - Bearer 타입이 아님")
    void resolveToken_NotBearerType_ReturnsNull() {
        // given
        given(request.getHeader("Authorization")).willReturn("Basic username:password");

        // when
        String result = jwtTokenProvider.resolveToken(request);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("토큰 파싱 성공")
    void parseClaims_ValidToken_ReturnsClaims() {
        // given
        String email = "test@example.com";
        Long memberId = 1L;
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        AuthTokenDTO tokenDto = jwtTokenProvider.generateToken(email, memberId, authorities);

        // when
        Claims claims = jwtTokenProvider.parseClaims(tokenDto.accessToken());

        // then
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(String.valueOf(memberId));
        assertThat(claims.get("auth")).isEqualTo("ROLE_USER");
        assertThat(claims.get("type")).isEqualTo("access");
    }
}