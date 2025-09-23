package com.example.nomodel._core.config;

import com.example.nomodel._core.security.oauth2.OAuth2SuccessHandler;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 테스트용 OAuth2 설정
 * SecurityConfig의 OAuth2 설정을 위한 더미 빈 제공
 */
@TestConfiguration
public class TestOAuth2Config {

    @Bean
    @Primary
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration testClient = ClientRegistration.withRegistrationId("test")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/test")
                .authorizationUri("http://localhost/oauth/authorize")
                .tokenUri("http://localhost/oauth/token")
                .userInfoUri("http://localhost/user")
                .userNameAttributeName("name")
                .build();

        return new InMemoryClientRegistrationRepository(testClient);
    }

    /*
        * OAuth2SuccessHandler를 Mockito Mock으로 대체
     */
    @Bean
    @Primary
    public OAuth2SuccessHandler oAuth2SuccessHandler() {
        return Mockito.mock(OAuth2SuccessHandler.class);
    }
}