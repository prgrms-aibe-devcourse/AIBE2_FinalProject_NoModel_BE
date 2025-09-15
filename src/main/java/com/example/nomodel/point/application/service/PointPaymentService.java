package com.example.nomodel.point.application.service;

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
public class PointPaymentService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${portone.api-key}")
    private String apiKey;

    @Value("${portone.api-secret}")
    private String apiSecret;

    @Value("${portone.kakao.subscription-channel-key}")
    private String kakaoChannelKey;

    @Value("${portone.toss.subscription-channel-key}")
    private String tossChannelKey;

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
     * PortOne 일회성 결제 API 호출 (포인트 충전용)
     * @param memberId 회원 ID
     * @param amount 결제 금액
     * @param channelKey 결제 채널 (카카오 / 토스)
     * @return 결제 성공 시 merchant_uid, 실패 시 null
     */
    public String processOneTimePayment(Long memberId, BigDecimal amount, String channelKey) {
        String token = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String merchantUid = "point_charge_" + memberId + "_" + System.currentTimeMillis();

        Map<String, Object> body = new HashMap<>();
        body.put("merchant_uid", merchantUid);
        body.put("amount", amount);
        body.put("pg", channelKey);
        body.put("name", "포인트 충전 " + amount + "원");
        body.put("buyer_name", "회원" + memberId);
        body.put("buyer_email", "member" + memberId + "@nomodel.com");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.iamport.kr/payments/prepare",
                    entity,
                    Map.class
            );

            Map<String, Object> result = (Map<String, Object>) response.getBody().get("response");
            if (result != null) {
                return merchantUid;
            }
        } catch (Exception e) {
            // 로깅은 AOP에서 처리
        }

        return null;
    }

    /**
     * 결제 검증
     * @param merchantUid 주문번호
     * @return 결제 성공 여부와 결제 정보
     */
    public PaymentVerificationResult verifyPayment(String merchantUid) {
        String token = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.iamport.kr/payments/" + merchantUid,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> result = (Map<String, Object>) response.getBody().get("response");
            if (result != null) {
                String status = (String) result.get("status");
                BigDecimal amount = new BigDecimal(result.get("amount").toString());

                return new PaymentVerificationResult(
                    "paid".equals(status),
                    amount,
                    merchantUid,
                    (String) result.get("imp_uid")
                );
            }
        } catch (Exception e) {
            // 로깅은 AOP에서 처리
        }

        return new PaymentVerificationResult(false, BigDecimal.ZERO, merchantUid, null);
    }

    /**
     * 카카오페이 포인트 충전 결제
     */
    public String processKakaoPointCharge(Long memberId, BigDecimal amount) {
        return processOneTimePayment(memberId, amount, kakaoChannelKey);
    }

    /**
     * 토스페이 포인트 충전 결제
     */
    public String processTossPointCharge(Long memberId, BigDecimal amount) {
        return processOneTimePayment(memberId, amount, tossChannelKey);
    }

    /**
     * 결제 검증 결과 클래스
     */
    public static class PaymentVerificationResult {
        private final boolean success;
        private final BigDecimal amount;
        private final String merchantUid;
        private final String impUid;

        public PaymentVerificationResult(boolean success, BigDecimal amount, String merchantUid, String impUid) {
            this.success = success;
            this.amount = amount;
            this.merchantUid = merchantUid;
            this.impUid = impUid;
        }

        public boolean isSuccess() { return success; }
        public BigDecimal getAmount() { return amount; }
        public String getMerchantUid() { return merchantUid; }
        public String getImpUid() { return impUid; }
    }
}