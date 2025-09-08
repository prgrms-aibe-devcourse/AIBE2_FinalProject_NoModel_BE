package com.example.nomodel._core.controller;

import com.example.nomodel._core.aop.annotation.BusinessCritical;
import com.example.nomodel._core.aop.annotation.BusinessCritical.BusinessDomain;
import com.example.nomodel._core.aop.annotation.BusinessCritical.CriticalLevel;
import com.example.nomodel._core.utils.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
@Tag(name = "API Demo", description = "비즈니스 중요도별 API 데모")
@BusinessCritical(domain = BusinessDomain.GENERAL, level = CriticalLevel.MEDIUM, description = "API 데모 컨트롤러")
public class ApiDemoController {

    @Operation(summary = "일반 API", description = "비즈니스 중요도가 낮은 일반 API (클래스 레벨 어노테이션 적용)")
    @GetMapping("/general")
    public ResponseEntity<ApiUtils.ApiResult<Map<String, Object>>> getGeneral() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "일반 API 호출 성공");
        response.put("businessType", "GENERAL");
        response.put("criticalLevel", "MEDIUM");
        
        return ResponseEntity.ok(ApiUtils.success(response));
    }

    @Operation(summary = "인증 API", description = "인증 관련 중요 API")
    @BusinessCritical(domain = BusinessDomain.AUTH, level = CriticalLevel.HIGH, description = "사용자 인증 처리")
    @PostMapping("/auth/login")
    public ResponseEntity<ApiUtils.ApiResult<Map<String, Object>>> login(@RequestBody Map<String, String> loginRequest) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "로그인 성공");
        response.put("businessType", "AUTH");
        response.put("criticalLevel", "HIGH");
        response.put("userId", "demo-user-123");
        
        // 의도적인 처리 시간 (성능 모니터링 테스트용)
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return ResponseEntity.ok(ApiUtils.success(response));
    }

    @Operation(summary = "결제 API", description = "결제 처리 매우 중요 API")
    @BusinessCritical(domain = BusinessDomain.PAYMENT, level = CriticalLevel.CRITICAL, description = "결제 트랜잭션 처리")
    @PostMapping("/payment/process")
    public ResponseEntity<ApiUtils.ApiResult<Map<String, Object>>> processPayment(@RequestBody Map<String, Object> paymentRequest) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "결제 처리 성공");
        response.put("businessType", "PAYMENT");
        response.put("criticalLevel", "CRITICAL");
        response.put("transactionId", "TXN-" + System.currentTimeMillis());
        response.put("amount", paymentRequest.get("amount"));
        
        // 의도적인 처리 시간 (성능 모니터링 테스트용)
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return ResponseEntity.ok(ApiUtils.success(response));
    }

    @Operation(summary = "사용자 정보 조회", description = "사용자 정보 조회 API")
    @BusinessCritical(domain = BusinessDomain.USER, level = CriticalLevel.MEDIUM, description = "사용자 프로필 조회")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiUtils.ApiResult<Map<String, Object>>> getUserInfo(@PathVariable String userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "사용자 정보 조회 성공");
        response.put("businessType", "USER");
        response.put("criticalLevel", "MEDIUM");
        response.put("userId", userId);
        response.put("userName", "Demo User");
        response.put("email", "demo@example.com");
        
        return ResponseEntity.ok(ApiUtils.success(response));
    }

    @Operation(summary = "보안 감사 API", description = "보안 이벤트 로깅 API")
    @BusinessCritical(domain = BusinessDomain.SECURITY, level = CriticalLevel.HIGH, description = "보안 감사 로그 생성")
    @PostMapping("/security/audit")
    public ResponseEntity<ApiUtils.ApiResult<Map<String, Object>>> securityAudit(@RequestBody Map<String, String> auditRequest) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "보안 감사 로그 생성 완료");
        response.put("businessType", "SECURITY");
        response.put("criticalLevel", "HIGH");
        response.put("auditId", "AUDIT-" + System.currentTimeMillis());
        response.put("action", auditRequest.get("action"));
        
        return ResponseEntity.ok(ApiUtils.success(response));
    }

    @Operation(summary = "에러 테스트 API", description = "에러 처리 테스트용 API")
    @BusinessCritical(domain = BusinessDomain.GENERAL, level = CriticalLevel.LOW, description = "에러 시나리오 테스트")
    @GetMapping("/error/test")
    public ResponseEntity<ApiUtils.ApiResult<Map<String, Object>>> errorTest(@RequestParam(defaultValue = "false") boolean throwError) {
        if (throwError) {
            throw new RuntimeException("의도적인 테스트 에러 발생");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "에러 테스트 성공");
        response.put("businessType", "GENERAL");
        response.put("criticalLevel", "LOW");
        
        return ResponseEntity.ok(ApiUtils.success(response));
    }

    @Operation(summary = "느린 API 테스트", description = "성능 모니터링 테스트용 느린 API")
    @GetMapping("/slow/test")
    public ResponseEntity<ApiUtils.ApiResult<?>> slowTest(@RequestParam(defaultValue = "1000") int delayMs) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError()
                .body(ApiUtils.error("인터럽트 발생", HttpStatus.INTERNAL_SERVER_ERROR));
        }
        
        response.put("message", "느린 API 테스트 완료");
        response.put("delayMs", delayMs);
        response.put("businessType", "GENERAL (클래스 레벨)");
        response.put("criticalLevel", "MEDIUM (클래스 레벨)");
        
        return ResponseEntity.ok(ApiUtils.success(response));
    }
}