package com.example.nomodel._core.demo;

import com.example.nomodel._core.utils.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AOP 기능 데모를 위한 컨트롤러
 * 실제 운영에서는 제거하고, 각각의 비즈니스 컨트롤러에서 AOP 기능을 활용하세요.
 * 
 * 테스트 URL 예시:
 * - GET /api/demo/aop/performance?input=test
 * - GET /api/demo/aop/user/123?requestType=profile
 * - POST /api/demo/aop/payment
 * - GET /api/demo/aop/products?category=electronics&page=0&size=10
 */
@Slf4j
@RestController
@RequestMapping("/demo/aop")
@RequiredArgsConstructor
@Tag(name = "AOP Demo", description = "AOP 기능 데모 API")
public class AopDemoController {

    private final AopDemoService aopDemoService;

    /**
     * Service 성능 모니터링 데모
     * ServicePerformanceAspect가 실행 시간을 자동 측정하고 로깅
     */
    @GetMapping("/performance")
    @Operation(summary = "Service 성능 모니터링 데모", 
               description = "ServicePerformanceAspect가 메소드 실행 시간을 측정하고 임계값 초과 시 경고 로그를 출력합니다.")
    public ResponseEntity<ApiUtils.ApiResult<String>> performanceDemo(
            @Parameter(description = "테스트 입력값") @RequestParam String input) {
        
        log.info("성능 모니터링 데모 API 호출: {}", input);
        
        String result = aopDemoService.performanceDemo(input);
        
        return ResponseEntity.ok(ApiUtils.success(result));
    }

    /**
     * 사용자 정보 조회 - 감사 로깅 데모 (MEDIUM 레벨)
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "사용자 정보 조회 (감사 로깅 데모)", 
               description = "@Auditable 어노테이션을 통한 비즈니스 로직 감사 로깅 데모입니다.")
    public ResponseEntity<ApiUtils.ApiResult<Map<String, Object>>> getUserInfo(
            @Parameter(description = "사용자 ID") @PathVariable Long userId,
            @Parameter(description = "요청 타입") @RequestParam(defaultValue = "profile") String requestType) {
        
        log.info("사용자 정보 조회 API 호출: userId={}, requestType={}", userId, requestType);
        
        Map<String, Object> userInfo = aopDemoService.getUserInfo(userId, requestType);
        
        return ResponseEntity.ok(ApiUtils.success(userInfo));
    }

    /**
     * 결제 처리 - 감사 로깅 데모 (HIGH 레벨)
     */
    @PostMapping("/payment")
    @Operation(summary = "결제 처리 (중요도 높은 감사 로깅 데모)", 
               description = "HIGH 레벨의 @Auditable 어노테이션을 통한 중요한 비즈니스 로직 감사 로깅 데모입니다.")
    public ResponseEntity<ApiUtils.ApiResult<String>> processPayment(
            @RequestBody PaymentRequest request) {
        
        log.info("결제 처리 API 호출: userId={}, amount={}", request.userId(), request.amount());
        
        try {
            String transactionId = aopDemoService.processPayment(
                request.userId(), 
                request.paymentMethod(), 
                request.amount()
            );
            
            return ResponseEntity.ok(ApiUtils.success(transactionId));
            
        } catch (RuntimeException e) {
            log.error("결제 처리 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiUtils.error(e.getMessage(), "PAYMENT_FAILED"));
        }
    }

    /**
     * 상품 목록 조회 - 감사 로깅 데모 (LOW 레벨)
     */
    @GetMapping("/products")
    @Operation(summary = "상품 목록 조회 (낮은 중요도 감사 로깅)", 
               description = "LOW 레벨의 @Auditable 어노테이션을 통한 단순 조회 작업 감사 로깅 데모입니다.")
    public ResponseEntity<ApiUtils.ApiResult<List<Map<String, Object>>>> getProductList(
            @Parameter(description = "상품 카테고리") @RequestParam(defaultValue = "electronics") String category,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {
        
        log.info("상품 목록 조회 API 호출: category={}, page={}, size={}", category, page, size);
        
        List<Map<String, Object>> products = aopDemoService.getProductList(category, page, size);
        
        return ResponseEntity.ok(ApiUtils.success(products));
    }

    /**
     * 비밀번호 변경 - 민감한 정보 감사 로깅 데모
     */
    @PutMapping("/password")
    @Operation(summary = "비밀번호 변경 (민감 정보 감사 로깅)", 
               description = "민감한 정보가 포함된 메소드의 감사 로깅 데모입니다. 파라미터와 결과가 마스킹됩니다.")
    public ResponseEntity<ApiUtils.ApiResult<String>> changePassword(
            @RequestBody PasswordChangeRequest request) {
        
        log.info("비밀번호 변경 API 호출: userId={}", request.userId());
        
        try {
            boolean success = aopDemoService.changePassword(
                request.userId(), 
                request.oldPassword(), 
                request.newPassword()
            );
            
            if (success) {
                return ResponseEntity.ok(ApiUtils.success("비밀번호가 성공적으로 변경되었습니다"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiUtils.error("비밀번호 변경 실패", "PASSWORD_CHANGE_FAILED"));
            }
            
        } catch (RuntimeException e) {
            log.error("비밀번호 변경 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiUtils.error(e.getMessage(), "PASSWORD_CHANGE_ERROR"));
        }
    }

    /**
     * 데이터 검증 - 예외 발생 감사 로깅 데모
     */
    @PostMapping("/validate")
    @Operation(summary = "데이터 검증 (예외 발생 감사 로깅)", 
               description = "예외 발생 시의 감사 로깅 동작을 확인할 수 있는 데모입니다.")
    public ResponseEntity<ApiUtils.ApiResult<String>> validateData(
            @Parameter(description = "검증할 데이터") @RequestParam String data) {
        
        log.info("데이터 검증 API 호출: data length={}", data != null ? data.length() : 0);
        
        try {
            aopDemoService.validateData(data);
            return ResponseEntity.ok(ApiUtils.success("데이터 검증 완료"));
            
        } catch (IllegalArgumentException e) {
            log.error("데이터 검증 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiUtils.error(e.getMessage(), "VALIDATION_ERROR"));
        }
    }

    /**
     * 느린 쿼리 시뮬레이션 데모
     * 실제로는 Repository에서 감지되지만, 데모 목적으로 Service에서 시뮬레이션
     */
    @GetMapping("/slow-query")
    @Operation(summary = "느린 쿼리 시뮬레이션", 
               description = "Slow Query 감지 기능을 테스트하기 위한 데모입니다. 1-3초의 지연이 발생합니다.")
    public ResponseEntity<ApiUtils.ApiResult<List<String>>> simulateSlowQuery(
            @Parameter(description = "검색어") @RequestParam String searchTerm) {
        
        log.info("느린 쿼리 시뮬레이션 API 호출: {}", searchTerm);
        
        List<String> results = aopDemoService.simulateSlowDatabaseQuery(searchTerm);
        
        return ResponseEntity.ok(ApiUtils.success(results));
    }

    /**
     * 결제 요청 DTO
     */
    public record PaymentRequest(
        Long userId,
        String paymentMethod,
        Double amount
    ) {}

    /**
     * 비밀번호 변경 요청 DTO
     */
    public record PasswordChangeRequest(
        Long userId,
        String oldPassword,
        String newPassword
    ) {}
}