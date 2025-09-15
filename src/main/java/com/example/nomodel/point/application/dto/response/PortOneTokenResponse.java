package com.example.nomodel.point.application.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PortOneTokenResponse {
    private int code;
    private String message;
    private ResponseData response;

    @Getter
    @Setter
    @ToString
    public static class ResponseData {
        private String access_token;
        private long now;
        private long expired_at;
    }
}
