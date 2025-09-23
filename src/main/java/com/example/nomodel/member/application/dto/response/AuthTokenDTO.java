package com.example.nomodel.member.application.dto.response;

public record AuthTokenDTO(
        String grantType,
        String accessToken,
        Long accessTokenValidTime,
        String refreshToken,
        Long refreshTokenValidTime
) {
}
