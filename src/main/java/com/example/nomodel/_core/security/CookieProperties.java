package com.example.nomodel._core.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.cookie")
@Getter
@Setter
public class CookieProperties {
    private String sameSite = "Lax";  // 기본값
    private boolean secure = false;   // 기본값
}