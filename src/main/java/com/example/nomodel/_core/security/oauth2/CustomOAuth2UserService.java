package com.example.nomodel._core.security.oauth2;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.userinfo.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.user.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
  
  private final RestClient rest = RestClient.create();
  
  @Override
  public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
    OAuth2User user = super.loadUser(req);
    String regId = req.getClientRegistration().getRegistrationId();
    
    Map<String,Object> attr = new HashMap<>(user.getAttributes());
    String id = null, email = null, name = null, avatar = null;
    
    if ("google".equals(regId)) {
      id = (String) attr.get("sub");
      email = (String) attr.get("email");
      name = (String) attr.get("name");
      avatar = (String) attr.get("picture");
    } else if ("github".equals(regId)) {
      id = String.valueOf(attr.get("id"));
      email = (String) attr.get("email");
      name = (String) attr.getOrDefault("name", attr.get("login"));
      avatar = (String) attr.get("avatar_url");
      
      // 이메일 비공개인 경우 /user/emails 호출해 primary verified 이메일 획득
      if (email == null) {
        List<Map<String,Object>> emails = rest.get()
                .uri("https://api.github.com/user/emails")
                .header(HttpHeaders.AUTHORIZATION, "token " + req.getAccessToken().getTokenValue())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (emails != null && !emails.isEmpty()) {
          email = emails.stream()
                  .filter(e -> Boolean.TRUE.equals(e.get("primary")) && Boolean.TRUE.equals(e.get("verified")))
                  .map(e -> (String)e.get("email"))
                  .findFirst()
                  .orElse((String) emails.get(0).get("email"));
        }
      }
    } else {
      throw new ApplicationException(ErrorCode.UNSUPPORTED_PROVIDER);
    }
    
    if (email == null) {
      throw new ApplicationException(ErrorCode.EMAIL_REQUIRED);
    }
    
    // 통일된 필드 삽입
    attr.put("provider", regId);
    attr.put("providerId", id);
    attr.put("email", email);
    attr.put("name", name);
    attr.put("avatar", avatar);
    attr.put("id", id); // nameAttributeKey로 사용할 키
    
    return new DefaultOAuth2User(user.getAuthorities(), attr, "id");
  }
}
