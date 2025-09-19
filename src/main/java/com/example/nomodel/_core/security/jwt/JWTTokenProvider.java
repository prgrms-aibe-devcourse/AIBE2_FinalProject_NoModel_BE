package com.example.nomodel._core.security.jwt;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel.member.application.dto.response.AuthTokenDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JWTTokenProvider {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String AUTHORITIES_KEY = "auth";
    private static final String MEMBER_ID_KEY = "memberId";
    private static final String BEARER_TYPE = "Bearer";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
    private static final String CLAIM_TYPE = "type";

    // jwt 토큰 암호화를 위한 키
    private final Key secretKey;
    // Access token 의 시간
    private final long accessTokenLifetime;
    // Refresh token 의 시간
    private final long refreshTokenLifetime;

    public JWTTokenProvider(@Value("${jwt.secret}") String secretKey,
                           @Value("${jwt.access-token-lifetime}") long accessTokenLifetime,
                           @Value("${jwt.refresh-token-lifetime}") long refreshTokenLifetime) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenLifetime = accessTokenLifetime;
        this.refreshTokenLifetime = refreshTokenLifetime;
    }

    public AuthTokenDTO generateToken(String email, Long memberId, Collection<? extends GrantedAuthority> grantedAuthorities) {
        // 권한 확인
        String authorities = grantedAuthorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        // 현재 시간
        Date now = new Date();

        // Access 토큰 제작
        String accessToken = Jwts.builder()
                // memberId를 subject로 사용 (불변값)
                .setSubject(String.valueOf(memberId))
                // 권한 주입
                .claim(AUTHORITIES_KEY, authorities)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                // 토큰 발행 시간 정보
                .setIssuedAt(now)
                // 만료시간 주입
                .setExpiration(new Date(now.getTime() + accessTokenLifetime * 1000))
                // 암호화
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        // Refresh 토큰 제작
        String refreshToken = Jwts.builder()
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshTokenLifetime * 1000))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        return new AuthTokenDTO(BEARER_TYPE, accessToken, accessTokenLifetime * 1000, refreshToken, refreshTokenLifetime * 1000);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException e) {
            log.error("JWT 서명이 유효하지 않습니다.", e);
            return false;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.error("JWT 토큰이 유효하지 않습니다.", e);
            return false;
        } catch (ExpiredJwtException e) {
            log.error("JWT 토큰이 만료되었습니다.", e);
            return false;
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            log.error("지원하지 않는 JWT 토큰입니다.", e);
            return false;
        } catch (IllegalArgumentException e) {
            log.error("JWT claims가 비어있습니다.", e);
            return false;
        }
    }

    public Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT Token 입니다.", e);
            return e.getClaims();
        }
    }

    // JWT 토큰 복호화 -> 토큰 정보 확인
    public Authentication getAuthentication(String token) {

        Claims claims = parseClaims(token);

        // 권한 정보가 없으면 예외
        if (claims.get(AUTHORITIES_KEY) == null) {
            throw new ApplicationException(ErrorCode.NO_AUTHORITIES_IN_TOKEN);
        }

        // 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // subject에서 memberId 가져오기
        Long memberId = Long.valueOf(claims.getSubject());

        // CustomUserDetails 객체를 Principal로 사용 (email은 필요 시 DB에서 조회)
        CustomUserDetails principal = new CustomUserDetails(memberId, null, "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    public boolean isRefreshToken(String token) {
        String type = (String) Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody().get(CLAIM_TYPE);
        return type.equals(TYPE_REFRESH);
    }

    public String resolveToken(HttpServletRequest request) {
        // 1순위: Authorization 헤더에서 토큰 추출 (기존 방식 유지)
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_TYPE) && bearerToken.length() > 7) {
            log.debug("토큰을 Authorization 헤더에서 추출: {}", bearerToken.substring(0, 20) + "...");
            return bearerToken.substring(7);  // 'Bearer ' 이후의 실제 토큰만 반환
        }

        // 2순위: 쿠키에서 Access Token 추출
        String tokenFromCookie = resolveTokenFromCookies(request, ACCESS_TOKEN_COOKIE_NAME);
        if (StringUtils.hasText(tokenFromCookie)) {
            log.debug("토큰을 쿠키에서 추출: {}", tokenFromCookie.substring(0, Math.min(20, tokenFromCookie.length())) + "...");
            return tokenFromCookie;
        }

        log.debug("토큰을 찾을 수 없음 - Authorization 헤더와 쿠키 모두 확인됨");
        return null;
    }

    /**
     * 쿠키에서 특정 토큰을 추출하는 메서드
     * @param request HTTP 요청
     * @param cookieName 찾을 쿠키 이름
     * @return 토큰 값 또는 null
     */
    public String resolveTokenFromCookies(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Refresh Token을 쿠키에서 추출하는 메서드
     * @param request HTTP 요청
     * @return Refresh Token 또는 null
     */
    public String resolveRefreshTokenFromCookies(HttpServletRequest request) {
        return resolveTokenFromCookies(request, REFRESH_TOKEN_COOKIE_NAME);
    }

}