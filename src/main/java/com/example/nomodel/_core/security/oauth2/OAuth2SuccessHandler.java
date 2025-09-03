package com.example.nomodel._core.security.oauth2;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel._core.security.jwt.JWTTokenProvider;
import com.example.nomodel.member.application.dto.response.AuthTokenDTO;
import com.example.nomodel.member.domain.model.*;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.member.domain.model.RefreshToken;
import com.example.nomodel.member.domain.repository.RefreshTokenRedisRepository;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
  
  private final MemberJpaRepository memberRepo;
  private final RefreshTokenRedisRepository refreshRepo;
  private final PasswordEncoder passwordEncoder;
  private final JWTTokenProvider jwt;
  private final org.springframework.security.oauth2.client.OAuth2AuthorizedClientService authorizedClientService;
  
  @Value("${app.frontend-origin:http://localhost:5173}")
  private String frontendOrigin;
  
  @Value("${app.oauth2-callback-path:/oauth2/callback}")
  private String callbackPath;
  
  @Override
  @Transactional
  public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth) throws IOException {
    var oauth2 = (org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) auth;
    String provider = oauth2.getAuthorizedClientRegistrationId(); // "google" | "github"
    var o = (org.springframework.security.oauth2.core.user.OAuth2User) auth.getPrincipal();
    var a = o.getAttributes();
    
    String providerId, email, name, avatar;
    
    if ("google".equals(provider)) {
      providerId = (String) a.get("sub");            // 고유 ID
      email      = (String) a.get("email");
      name       = (String) a.get("name");
      avatar     = (String) a.get("picture");
    } else if ("github".equals(provider)) {
      providerId = String.valueOf(a.get("id"));      // 고유 ID
      email      = (String) a.get("email");          // 비공개면 null 가능
      name       = (String) a.getOrDefault("name", a.get("login"));
      avatar     = (String) a.get("avatar_url");
      
      // (선택) 이메일 보강: 깃허브가 null이면 /user/emails 호출
      if (email == null || email.isBlank()) {
        var client = authorizedClientService.loadAuthorizedClient(provider, oauth2.getName());
        if (client != null) {
          email = fetchGithubPrimaryEmail(client.getAccessToken().getTokenValue());
        }
      }
    } else {
      throw new ApplicationException(ErrorCode.UNSUPPORTED_PROVIDER);
    }
    
    // 1) 회원 조회/없으면 생성 (패스워드는 임의 생성)
    String finalEmail = email;
    Member member = memberRepo.findByEmail(Email.of(email))
            .orElseGet(() -> {
              String username = (name != null && !name.isBlank())
                      ? name
                      : (provider + "_" + providerId);
              String rawPw = UUID.randomUUID() + "!" + System.nanoTime();
              return memberRepo.save(
                      Member.createMember(
                              username,
                              Email.of(finalEmail),
                              Password.encode(rawPw, passwordEncoder)
                      )
              );
            });
    
    // 2) 권한 만들기 (기본 USER)
    var authorities = List.of(new SimpleGrantedAuthority(member.getRole().getKey()));
    
    // 3) JWT 발급 (AuthTokenDTO: grantType, accessToken, accessTokenValidTime, refreshToken, refreshTokenValidTime)
    AuthTokenDTO tokens = jwt.generateToken(email, member.getId(), authorities);
    
    // 4) 리프레시 토큰 저장/갱신 (Redis)
    RefreshToken refresh = RefreshToken.builder()
            .id(String.valueOf(member.getId()))
            .authorities(authorities)
            .refreshToken(tokens.refreshToken())
            .build();
    refreshRepo.save(refresh);
    
    // 5) 프론트 콜백으로 전달
    String url = frontendOrigin + callbackPath
            + "#access=" + URLEncoder.encode(tokens.accessToken(), StandardCharsets.UTF_8)
            + "&refresh=" + URLEncoder.encode(tokens.refreshToken(), StandardCharsets.UTF_8);
    res.sendRedirect(url);
  }
  
  private String fetchGithubPrimaryEmail(String accessToken) {
    try {
      var rest = org.springframework.web.client.RestClient.create();
      var emails = rest.get()
              .uri("https://api.github.com/user/emails")
              .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "token " + accessToken)
              .retrieve()
              .body(new org.springframework.core.ParameterizedTypeReference<java.util.List<java.util.Map<String,Object>>>() {});
      if (emails == null || emails.isEmpty()) return null;
      return emails.stream()
              .filter(e -> Boolean.TRUE.equals(e.get("primary")) && Boolean.TRUE.equals(e.get("verified")))
              .map(e -> (String) e.get("email"))
              .findFirst()
              .orElse((String) emails.get(0).get("email"));
    } catch (Exception ignored) {
      return null;
    }
  }
}
