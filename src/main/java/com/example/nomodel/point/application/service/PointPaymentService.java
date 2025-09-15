package com.example.nomodel.point.application.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.point.application.dto.response.PortOneTokenResponse;
import com.example.nomodel.point.domain.repository.PointTransactionRepository;
import com.example.nomodel.point.domain.service.PointDomainService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PointPaymentService {

    private final PointDomainService pointDomainService;
    private final PointTransactionRepository transactionRepository;

    @Value("${portone.api-key}")
    private String apiKey;

    @Value("${portone.api-secret}")
    private String apiSecret;

    @Value("${portone.kakao.normal-channel-key}")
    private String kakaoNormalChannelKey;

    private final String IAMPORT_API_BASE_URL = "https://api.iamport.kr";
    private RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        System.out.println("⭐ PointPaymentService 초기화 완료 ⭐");
        System.out.println("PortOne API Key: " + (apiKey != null && !apiKey.isEmpty() ? "******" : "❌ 설정되지 않음"));
        System.out.println("PortOne API Secret: " + (apiSecret != null && !apiSecret.isEmpty() ? "******" : "❌ 설정되지 않음"));
        System.out.println("Kakao Normal Channel Key: " + (kakaoNormalChannelKey != null && !kakaoNormalChannelKey.isEmpty() ? "******" : "❌ 설정되지 않음"));
    }

    /**
     * PortOne AccessToken 발급
     */
    public String getAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("imp_key", apiKey);
        body.put("imp_secret", apiSecret);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<PortOneTokenResponse> response = restTemplate.postForEntity(
                    IAMPORT_API_BASE_URL + "/users/getToken", entity, PortOneTokenResponse.class);

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && response.getBody().getCode() == 0
                    && response.getBody().getResponse() != null) {
                return response.getBody().getResponse().getAccess_token();
            } else {
                throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
            }
        } catch (Exception e) {
            throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
    }

    /**
     * 결제 사전 등록 (프론트엔드 결제창 호출 전)
     */
    public String preparePayment(BigDecimal amount) {
        String merchantUid = "point_charge_" + UUID.randomUUID();

        Map<String, Object> body = new HashMap<>();
        body.put("merchant_uid", merchantUid);
        body.put("amount", amount);

        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    IAMPORT_API_BASE_URL + "/payments/prepare", entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Integer code = (Integer) response.getBody().get("code");
                if (code != null && code == 0) {
                    return merchantUid;
                }
            }
            throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        } catch (Exception e) {
            throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
    }

    /**
     * 결제 검증 및 포인트 충전
     */
    public PaymentVerificationResult verifyPayment(String impUid, String merchantUid, Long memberId) {
        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    IAMPORT_API_BASE_URL + "/payments/{imp_uid}",
                    HttpMethod.GET,
                    entity,
                    Map.class,
                    impUid
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Integer code = (Integer) response.getBody().get("code");
                if (code != null && code == 0) {
                    Map<String, Object> paymentData = (Map<String, Object>) response.getBody().get("response");
                    if (paymentData == null) {
                        throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
                    }

                    String status = (String) paymentData.get("status");
                    String responseMerchantUid = (String) paymentData.get("merchant_uid");
                    BigDecimal amount = new BigDecimal(paymentData.get("amount").toString());

                    if (!"paid".equals(status)) {
                        throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
                    }
                    if (!merchantUid.equals(responseMerchantUid)) {
                        throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
                    }

                    // 포인트 충전
                    pointDomainService.chargePoints(memberId, amount, impUid).block();

                    return new PaymentVerificationResult(true, amount, merchantUid, impUid);
                }
            }
            throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        } catch (Exception e) {
            throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
    }

    // 내부 DTO
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
