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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

    @Value("${portone.imp-key}")
    private String apiKey;

    @Value("${portone.imp-secret}")
    private String apiSecret;

    @Value("${portone.kakao.normal-channel-key}")
    private String kakaoNormalChannelKey;

    private final String IAMPORT_API_BASE_URL = "https://api.iamport.kr";
    private RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        System.out.println("==== PointPaymentService INIT CALLED ====");
        System.out.println("apiKey=" + apiKey);
        System.out.println("apiSecret=" + apiSecret);
        System.out.println("kakaoNormalChannelKey=" + kakaoNormalChannelKey);
    }

    /**
     * PortOne AccessToken 발급
     */
    private String getAccessToken() {
        System.out.println("🔍 PortOne 액세스 토큰 취득 요청 시작...");

        try {
            // MultiValueMap 사용 (form-urlencoded 방식)
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("imp_key", apiKey);
            body.add("imp_secret", apiSecret);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED); // 이 부분이 핵심

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            System.out.println("Content-Type을 form-urlencoded로 변경");
            System.out.println("요청 URL: " + IAMPORT_API_BASE_URL + "/users/getToken");

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    IAMPORT_API_BASE_URL + "/users/getToken",
                    entity,
                    Map.class
            );

            // 나머지 로직은 동일...
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Integer code = (Integer) response.getBody().get("code");
                if (code != null && code == 0) {
                    Map<String, Object> responseData = (Map<String, Object>) response.getBody().get("response");
                    if (responseData != null) {
                        String accessToken = (String) responseData.get("access_token");
                        System.out.println("✅ 액세스 토큰 획득 성공");
                        return accessToken;
                    }
                }
            }

            System.err.println("❌ 액세스 토큰 획득 실패");
            throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);

        } catch (Exception e) {
            System.err.println("❌ 전체 오류: " + e.getMessage());
            e.printStackTrace();
            throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
    }



    public PaymentVerificationResult verifyPayment(String impUid, String merchantUid, Long memberId) {
        System.out.println("🔍 PortOne 결제 검증 요청 시작...");
        System.out.println("impUid: " + impUid + ", merchantUid: " + merchantUid);

        try {
            // 액세스 토큰 획득 및 검증
            String accessToken = getAccessToken();
            if (accessToken == null || accessToken.trim().isEmpty()) {
                System.err.println("❌ 액세스 토큰 획득 실패");
                throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
            }
            System.out.println("✅ 액세스 토큰 획득 성공: " + accessToken.substring(0, 20) + "...");

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // PortOne API 호출
            String apiUrl = IAMPORT_API_BASE_URL + "/payments/" + impUid;
            System.out.println("API 호출 URL: " + apiUrl);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            System.out.println("응답 상태 코드: " + response.getStatusCode());
            System.out.println("응답 본문: " + response.getBody());

            // 응답 검증
            if (!response.getStatusCode().is2xxSuccessful()) {
                System.err.println("❌ HTTP 응답 실패: " + response.getStatusCode());
                throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
            }

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                System.err.println("❌ 응답 본문이 null입니다");
                throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
            }

            // PortOne 응답 코드 확인
            Integer code = (Integer) responseBody.get("code");
            if (code == null || code != 0) {
                String message = (String) responseBody.get("message");
                System.err.println("❌ PortOne API 오류 - 코드: " + code + ", 메시지: " + message);
                throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
            }

            // 결제 데이터 추출
            Map<String, Object> paymentData = (Map<String, Object>) responseBody.get("response");
            if (paymentData == null) {
                System.err.println("❌ 결제 데이터가 없습니다");
                throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
            }

            String status = (String) paymentData.get("status");
            String responseMerchantUid = (String) paymentData.get("merchant_uid");
            Object amountObj = paymentData.get("amount");

            System.out.println("결제 상태: " + status);
            System.out.println("merchant_uid: " + responseMerchantUid);
            System.out.println("결제 금액: " + amountObj);

            // 결제 상태 검증
            if (!"paid".equals(status)) {
                System.err.println("❌ 결제 상태가 'paid'가 아닙니다. 현재 상태: " + status);
                throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
            }

            // merchant_uid 일치 확인
            if (!merchantUid.equals(responseMerchantUid)) {
                System.err.println("❌ merchant_uid 불일치. 요청: " + merchantUid + ", 응답: " + responseMerchantUid);
                throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
            }

            // 금액 변환
            BigDecimal amount;
            if (amountObj instanceof Number) {
                amount = new BigDecimal(amountObj.toString());
            } else {
                System.err.println("❌ 결제 금액이 숫자가 아닙니다: " + amountObj);
                throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
            }

            System.out.println("✅ 결제 검증 성공 - 금액: " + amount + "원");

            // 포인트 충전
            try {
                pointDomainService.chargePoints(memberId, amount, impUid).block();
                System.out.println("✅ 포인트 충전 완료");
            } catch (Exception e) {
                System.err.println("❌ 포인트 충전 실패: " + e.getMessage());
                throw new ApplicationException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
            }

            return new PaymentVerificationResult(true, amount, merchantUid, impUid);

        } catch (ApplicationException e) {
            // 이미 정의된 ApplicationException은 그대로 재던짐
            throw e;
        } catch (Exception e) {
            System.err.println("❌ 결제 검증 중 예외 발생: " + e.getMessage());
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
