package com.example.nomodel._core.security.oauth2;

import com.example.nomodel._core.security.jwt.JWTTokenProvider;
import com.example.nomodel.member.application.dto.response.AuthTokenDTO;
import com.example.nomodel.member.domain.model.Email;
import com.example.nomodel.member.domain.model.Member;
import com.example.nomodel.member.domain.model.Password;
import com.example.nomodel.member.domain.model.RefreshToken;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.member.domain.repository.RefreshTokenRedisRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Profile("!test")  // 테스트 환경에서는 동작하지 않도록 설정
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
  
  private final MemberJpaRepository memberRepo;
  private final RefreshTokenRedisRepository refreshRepo;
  private final PasswordEncoder passwordEncoder;
  private final JWTTokenProvider jwt;
  
  @Value("${app.frontend-origin:http://localhost:5173}")
  private String frontendOrigin;
  
  @Value("${app.oauth2-callback-path:/oauth2/callback}")
  private String callbackPath;
  
  // ▼ 운영에선 None+Secure 권장, 로컬(http) 개발에선 Lax + Secure=false
  @Value("${app.cookie.same-site:Lax}")   // Lax | None
  private String sameSite;
  
  @Value("${app.cookie.secure:false}")    // prod: true (HTTPS만)
  private boolean secure;
  
  @Value("${app.cookie.domain:}")         // 예: example.com (없으면 자동)
  private String cookieDomain;
  
  private static final String ACCESS_COOKIE = "accessToken";
  private static final String REFRESH_COOKIE = "refreshToken";
  
  @Override
  @Transactional
  public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth) throws IOException, IOException {
    var oauth2 = (org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) auth;
    String provider = oauth2.getAuthorizedClientRegistrationId();
    var o = (org.springframework.security.oauth2.core.user.OAuth2User) auth.getPrincipal();
    var a = o.getAttributes();
    
    String providerId, email, name;
    
    if ("google".equals(provider)) {
      providerId = (String) a.get("sub");
      email      = (String) a.get("email");
      name       = (String) a.get("name");
    } else if ("github".equals(provider)) {
      providerId = String.valueOf(a.get("id"));
      email      = (String) a.get("email"); // 필요 시 /user/emails 보강
      name       = (String) a.getOrDefault("name", a.get("login"));
    } else {
      throw new IllegalStateException("Unsupported provider: " + provider);
    }
    
    if (email == null || email.isBlank()) {
      // 이메일 필수 정책이면 에러 페이지로 보냄
      res.sendRedirect(frontendOrigin + "/oauth2/error?reason=email_required");
      return;
    }
    
    Member member = memberRepo.findByEmail(Email.of(email))
            .orElseGet(() -> {
              String username = (name != null && !name.isBlank()) ? name : (provider + "_" + providerId);
              String rawPw = java.util.UUID.randomUUID() + "!" + System.nanoTime();
              return memberRepo.save(
                      Member.createMember(username, Email.of(email), Password.encode(rawPw, passwordEncoder))
              );
            });
    
    var authorities = java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(member.getRole().getKey()));
    AuthTokenDTO tokens = jwt.generateToken(email, member.getId(), authorities);
    
    // 리프레시 토큰 저장(예: Redis)
    refreshRepo.save(
            RefreshToken.builder()
                    .id(String.valueOf(member.getId()))
                    .authorities(authorities)
                    .refreshToken(tokens.refreshToken())
                    .build()
    );
    
    // === 쿠키 설정 (HttpOnly 권장) ===
    addCookie(res, ACCESS_COOKIE,  tokens.accessToken(),  tokens.accessTokenValidTime());
    addCookie(res, REFRESH_COOKIE, tokens.refreshToken(), tokens.refreshTokenValidTime());
    
    // 프론트 콜백으로 "깨끗하게" 리다이렉트 (URL에 토큰 X)
    res.sendRedirect(frontendOrigin + callbackPath);
  }
  
  private void addCookie(HttpServletResponse res, String name, String value, long maxAgeMs) {
    var b = org.springframework.http.ResponseCookie.from(name, value)
            .httpOnly(true)                 // JS 접근 차단
            .secure(secure)                 // prod: true (HTTPS 필수)
            .path("/");
    
    if (cookieDomain != null && !cookieDomain.isBlank()) {
      b.domain(cookieDomain);         // 예: example.com
    }
    
    // SameSite: 로컬(http) 개발이면 Lax, 서로 다른 사이트 간에는 None+Secure 필요
    b.sameSite(sameSite);             // "Lax" or "None"
    b.maxAge(java.time.Duration.ofMillis(maxAgeMs));
    
    res.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, b.build().toString());
  }
}
