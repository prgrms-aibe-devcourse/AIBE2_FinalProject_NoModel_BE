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
        System.out.println("PortOne API Key: " + (apiKey != null && !apiKey.isEmpty() ? apiKey.substring(0, Math.min(apiKey.length(), 4)) + "..." + apiKey.substring(Math.max(0, apiKey.length() - 4)) : "❌ 설정되지 않음"));
        System.out.println("PortOne API Secret: " + (apiSecret != null && !apiSecret.isEmpty() ? apiSecret.substring(0, Math.min(apiSecret.length(), 4)) + "..." + apiSecret.substring(Math.max(0, apiSecret.length() - 4)) : "❌ 설정되지 않음"));
        System.out.println("Kakao Normal Channel Key: " + (kakaoNormalChannelKey != null && !kakaoNormalChannelKey.isEmpty() ? kakaoNormalChannelKey.substring(0, Math.min(kakaoNormalChannelKey.length(), 4)) + "..." + kakaoNormalChannelKey.substring(Math.max(0, kakaoNormalChannelKey.length() - 4)) : "❌ 설정되지 않음"));
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

        System.out.println("🔑 PortOne 액세스 토큰 발급 요청 시작...");
        System.out.println("요청 URL: " + IAMPORT_API_BASE_URL + "/users/getToken");
        System.out.println("요청 헤더: " + headers);
        System.out.println("요청 본문: " + body);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<PortOneTokenResponse> response = restTemplate.postForEntity(
                    IAMPORT_API_BASE_URL + "/users/getToken", entity, PortOneTokenResponse.class);

            System.out.println("응답 상태 코드: " + response.getStatusCode());
            System.out.println("응답 헤더: " + response.getHeaders());
            System.out.println("응답 본문: " + response.getBody());

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && response.getBody().getCode() == 0
                    && response.getBody().getResponse() != null) {
                System.out.println("✅ PortOne 액세스 토큰 발급 성공.");
                return response.getBody().getResponse().getAccess_token();
            } else {
                String errorMessage = "PortOne 액세스 토큰 발급 실패: " + (response.getBody() != null ? response.getBody().getMessage() : "응답 본문 없음");
                System.err.println("❌ " + errorMessage);
                throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
            }
        } catch (Exception e) {
            System.err.println("❌ PortOne 토큰 발급 중 예외 발생: " + e.getMessage());
            e.printStackTrace();
            throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
    }

    /**
     * 결제 사전 등록 (프론트엔드 결제창 호출 전)
     * @param amount 결제 금액
     * @return 생성된 merchant_uid
     */
    public String preparePayment(BigDecimal amount) {
        String merchantUid = "point_charge_" + UUID.randomUUID().toString(); 

        Map<String, Object> body = new HashMap<>();
        body.put("merchant_uid", merchantUid);
        body.put("amount", amount);

        System.out.println("💰 PortOne 결제 사전 등록 요청 시작...");
        System.out.println("요청 본문: " + body);

        String accessToken = getAccessToken(); // 동기 호출

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    IAMPORT_API_BASE_URL + "/payments/prepare", entity, Map.class);

            System.out.println("응답 상태 코드: " + response.getStatusCode());
            System.out.println("응답 헤더: " + response.getHeaders());
            System.out.println("응답 본문: " + response.getBody()); 

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Integer code = (Integer) response.getBody().get("code");
                if (code != null && code == 0) {
                    System.out.println("✅ PortOne 결제 사전 등록 성공: " + merchantUid);
                    return merchantUid;
                } else {
                    String msg = (String) response.getBody().get("message");
                    System.err.println("❌ PortOne 결제 사전 등록 실패: " + msg);
                    throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
                }
            } else {
                String errorMessage = "PortOne 결제 사전 등록 실패: " + response.getStatusCode();
                System.err.println("❌ " + errorMessage);
                throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
            }
        } catch (Exception e) {
            System.err.println("❌ PortOne 결제 사전 등록 중 예외 발생: " + e.getMessage());
            e.printStackTrace();
            throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
    }

    /**
     * PortOne API 호출로 결제 검증 및 포인트 충전
     * @param impUid PortOne 결제 고유번호
     * @param merchantUid 가맹점 주문번호 (사전 등록 시 사용)
     * @param memberId 회원 ID
     * @return 결제 검증 성공 여부와 결제 정보
     */
    public PaymentVerificationResult verifyPayment(String impUid, String merchantUid, Long memberId) {
        System.out.println("🔍 PortOne 결제 검증 요청 시작...");
        System.out.println("impUid: " + impUid + ", merchantUid: " + merchantUid);

        String accessToken = getAccessToken(); // 동기 호출

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

            System.out.println("응답 상태 코드: " + response.getStatusCode());
            System.out.println("응답 헤더: " + response.getHeaders());
            System.out.println("응답 본문: " + response.getBody()); 

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Integer code = (Integer) response.getBody().get("code");
                if (code != null && code == 0) {
                    Map<String, Object> paymentData = (Map<String, Object>) response.getBody().get("response");
                    if (paymentData == null) {
                        System.err.println("❌ PortOne 결제 정보 없음");
                        throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
                    }

                    String status = (String) paymentData.get("status");
                    String responseMerchantUid = (String) paymentData.get("merchant_uid");
                    BigDecimal amount = new BigDecimal(paymentData.get("amount").toString());

                    System.out.println("✅ PortOne 결제 상태: " + status + ", 금액: " + amount + ", merchant_uid: " + responseMerchantUid);

                    // 1. 상태 및 merchant_uid 일치 여부 확인
                    if (!"paid".equals(status)) {
                        System.err.println("❌ 결제 상태가 'paid'가 아닙니다. 현재 상태: " + status);
                        throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
                    }
                    if (!merchantUid.equals(responseMerchantUid)) {
                        System.err.println("❌ merchant_uid 불일치. 요청: " + merchantUid + ", 응답: " + responseMerchantUid);
                        throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
                    }

                    // 2. 포인트 충전
                    pointDomainService.chargePoints(memberId, amount, impUid).block(); // Mono<Void>의 블로킹 호출

                    return new PaymentVerificationResult(true, amount, merchantUid, impUid);

                } else {
                    String msg = (String) response.getBody().get("message");
                    System.err.println("❌ PortOne 결제 검증 실패: " + msg);
                    throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
                }
            } else {
                String errorMessage = "PortOne 결제 검증 실패: " + response.getStatusCode();
                System.err.println("❌ " + errorMessage);
                throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
            }
        } catch (Exception e) {
            System.err.println("❌ PortOne 결제 검증 중 예외 발생: " + e.getMessage());
            e.printStackTrace();
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
