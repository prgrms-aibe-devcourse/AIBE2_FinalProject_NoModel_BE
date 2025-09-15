package com.example.nomodel.subscription.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PortOnePaymentService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${portone.api-key}")
    private String apiKey;

    @Value("${portone.api-secret}")
    private String apiSecret;

    @Value("${portone.kakao.subscription-channel-key}")
    private String kakaoChannelKey;

    /**
     * PortOne AccessToken 발급
     */
    private String getAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("imp_key", apiKey);
        body.put("imp_secret", apiSecret);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.iamport.kr/users/getToken", entity, Map.class
        );

        return (String) ((Map) response.getBody().get("response")).get("access_token");
    }

    /**
     * PortOne 정기결제 API 호출 (카카오 전용)
     * @param customerUid PortOne 빌링키
     * @param amount 결제 금액
     */
    public boolean processKakaoRecurring(String customerUid, BigDecimal amount) {
        String token = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("customer_uid", customerUid);
        body.put("merchant_uid", "order_" + System.currentTimeMillis()); // 고유 주문번호
        body.put("amount", amount);
        body.put("pg", kakaoChannelKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.iamport.kr/subscribe/payments/again",
                entity,
                Map.class
        );

        Map<String, Object> result = (Map<String, Object>) response.getBody().get("response");
        return result != null && "paid".equals(result.get("status"));
    }
}
