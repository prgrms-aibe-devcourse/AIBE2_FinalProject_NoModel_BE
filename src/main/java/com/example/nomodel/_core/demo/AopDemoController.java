package com.example.nomodel._core.demo;

import com.example.nomodel._core.aop.annotation.BusinessCritical;
import com.example.nomodel._core.aop.annotation.BusinessCritical.BusinessDomain;
import com.example.nomodel._core.aop.annotation.BusinessCritical.CriticalLevel;
import com.example.nomodel._core.utils.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AOP 기능 데모를 위한 컨트롤러
 * @BusinessCritical 어노테이션 기반의 새로운 AOP 시스템 데모
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
@Tag(name = "AOP Demo", description = "@BusinessCritical 어노테이션 기반 AOP 기능 데모 API")
@BusinessCritical(domain = BusinessDomain.GENERAL, level = CriticalLevel.LOW, description = "AOP 데모 컨트롤러")
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
     * 사용자 정보 조회 - 비즈니스 중요 API 데모 (MEDIUM 레벨)
     */
    @GetMapping("/user/{userId}")
    @BusinessCritical(domain = BusinessDomain.USER, level = CriticalLevel.MEDIUM, description = "사용자 정보 조회")
    @Operation(summary = "사용자 정보 조회 (비즈니스 중요 API 데모)", 
               description = "@BusinessCritical 어노테이션을 통한 사용자 도메인 비즈니스 로직 모니터링 데모입니다.")
    public ResponseEntity<ApiUtils.ApiResult<Map<String, Object>>> getUserInfo(
            @Parameter(description = "사용자 ID") @PathVariable Long userId,
            @Parameter(description = "요청 타입") @RequestParam(defaultValue = "profile") String requestType) {
        
        log.info("사용자 정보 조회 API 호출: userId={}, requestType={}", userId, requestType);
        
        Map<String, Object> userInfo = aopDemoService.getUserInfo(userId, requestType);
        
        return ResponseEntity.ok(ApiUtils.success(userInfo));
    }

    /**
     * 결제 처리 - 비즈니스 중요 API 데모 (CRITICAL 레벨)
     */
    @PostMapping("/payment")
    @BusinessCritical(domain = BusinessDomain.PAYMENT, level = CriticalLevel.CRITICAL, description = "결제 트랜잭션 처리")
    @Operation(summary = "결제 처리 (최고 중요도 비즈니스 API 데모)", 
               description = "CRITICAL 레벨의 @BusinessCritical 어노테이션을 통한 결제 도메인 비즈니스 로직 모니터링 데모입니다.")
    public ResponseEntity<ApiUtils.ApiResult<?>> processPayment(
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
                .body(ApiUtils.error(e.getMessage(), HttpStatus.BAD_REQUEST));
        }
    }

    /**
     * 상품 목록 조회 - 일반 API 데모 (클래스 레벨 적용)
     */
    @GetMapping("/products")
    @Operation(summary = "상품 목록 조회 (클래스 레벨 비즈니스 중요도)", 
               description = "클래스 레벨의 @BusinessCritical 어노테이션이 적용된 일반 조회 작업 데모입니다.")
    public ResponseEntity<ApiUtils.ApiResult<List<Map<String, Object>>>> getProductList(
            @Parameter(description = "상품 카테고리") @RequestParam(defaultValue = "electronics") String category,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {
        
        log.info("상품 목록 조회 API 호출: category={}, page={}, size={}", category, page, size);
        
        List<Map<String, Object>> products = aopDemoService.getProductList(category, page, size);
        
        return ResponseEntity.ok(ApiUtils.success(products));
    }

    /**
     * 비밀번호 변경 - 보안 도메인 비즈니스 중요 API 데모
     */
    @PutMapping("/password")
    @BusinessCritical(domain = BusinessDomain.SECURITY, level = CriticalLevel.HIGH, description = "비밀번호 변경")
    @Operation(summary = "비밀번호 변경 (보안 도메인 비즈니스 중요 API)", 
               description = "보안 도메인의 HIGH 레벨 @BusinessCritical 어노테이션이 적용된 민감한 정보 처리 데모입니다.")
    public ResponseEntity<ApiUtils.ApiResult<?>> changePassword(
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
                    .body(ApiUtils.error("비밀번호 변경 실패", HttpStatus.BAD_REQUEST));
            }
            
        } catch (RuntimeException e) {
            log.error("비밀번호 변경 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiUtils.error(e.getMessage(), HttpStatus.BAD_REQUEST));
        }
    }

    /**
     * 데이터 검증 - 예외 발생 비즈니스 로깅 데모
     */
    @PostMapping("/validate")
    @Operation(summary = "데이터 검증 (예외 발생 비즈니스 로깅)", 
               description = "예외 발생 시의 비즈니스 로깅 동작을 확인할 수 있는 데모입니다. 클래스 레벨 어노테이션 적용.")
    public ResponseEntity<ApiUtils.ApiResult<?>> validateData(
            @Parameter(description = "검증할 데이터") @RequestParam String data) {
        
        log.info("데이터 검증 API 호출: data length={}", data != null ? data.length() : 0);
        
        try {
            aopDemoService.validateData(data);
            return ResponseEntity.ok(ApiUtils.success("데이터 검증 완료"));
            
        } catch (IllegalArgumentException e) {
            log.error("데이터 검증 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiUtils.error(e.getMessage(), HttpStatus.BAD_REQUEST));
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