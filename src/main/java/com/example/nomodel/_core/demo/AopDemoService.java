package com.example.nomodel._core.demo;

import com.example.nomodel._core.aop.annotation.Auditable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AOP 기능 데모를 위한 서비스 클래스
 * 실제 프로젝트에서는 제거하고, 각각의 비즈니스 서비스에 적절한 AOP 어노테이션을 적용하세요.
 */
@Slf4j
@Service
public class AopDemoService {

    /**
     * Service 성능 모니터링 AOP 데모
     * - ServicePerformanceAspect가 자동으로 실행 시간을 측정하고 로깅
     * - 임계값 초과 시 경고 로그 출력
     */
    public String performanceDemo(String input) {
        log.info("Service 성능 모니터링 데모 시작: {}", input);
        
        // 랜덤하게 실행 시간 시뮬레이션 (100ms ~ 1500ms)
        int delay = ThreadLocalRandom.current().nextInt(100, 1500);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return "Performance demo completed with input: " + input + " (simulated " + delay + "ms)";
    }

    /**
     * 감사 로깅 AOP 데모 - 일반 중요도
     * @Auditable 어노테이션을 통한 비즈니스 로직 감사
     */
    @Auditable(
        action = "사용자 정보 조회",
        category = "USER",
        level = Auditable.AuditLevel.MEDIUM,
        logParameters = true,
        logResult = true
    )
    public Map<String, Object> getUserInfo(Long userId, String requestType) {
        log.info("사용자 정보 조회 시작: userId={}, requestType={}", userId, requestType);
        
        // 비즈니스 로직 시뮬레이션
        simulateProcessingTime(200);
        
        return Map.of(
            "userId", userId,
            "name", "Demo User " + userId,
            "email", "user" + userId + "@example.com",
            "requestType", requestType,
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 감사 로깅 AOP 데모 - 높은 중요도 (결제 등 중요한 비즈니스 로직)
     */
    @Auditable(
        action = "결제 처리",
        category = "PAYMENT",
        level = Auditable.AuditLevel.HIGH,
        logParameters = true,
        logResult = false  // 결제 결과는 민감 정보이므로 로깅 제외
    )
    public String processPayment(Long userId, String paymentMethod, Double amount) {
        log.info("결제 처리 시작: userId={}, method={}, amount={}", userId, paymentMethod, amount);
        
        // 결제 로직 시뮬레이션
        simulateProcessingTime(500);
        
        // 실패 케이스 시뮬레이션 (20% 확률)
        if (ThreadLocalRandom.current().nextInt(100) < 20) {
            throw new RuntimeException("결제 처리 실패: 카드 승인 거부");
        }
        
        String transactionId = "TXN_" + System.currentTimeMillis();
        log.info("결제 완료: transactionId={}", transactionId);
        
        return transactionId;
    }

    /**
     * 감사 로깅 AOP 데모 - 낮은 중요도 (조회 등 단순 작업)
     */
    @Auditable(
        action = "상품 목록 조회",
        category = "PRODUCT",
        level = Auditable.AuditLevel.LOW,
        logParameters = false,  // 조회 파라미터는 로깅하지 않음
        logResult = false       // 목록 결과도 로깅하지 않음
    )
    public List<Map<String, Object>> getProductList(String category, int page, int size) {
        log.info("상품 목록 조회: category={}, page={}, size={}", category, page, size);
        
        simulateProcessingTime(100);
        
        // 더미 상품 목록 반환
        return List.of(
            Map.of("id", 1, "name", "상품 A", "price", 10000, "category", category),
            Map.of("id", 2, "name", "상품 B", "price", 20000, "category", category),
            Map.of("id", 3, "name", "상품 C", "price", 30000, "category", category)
        );
    }

    /**
     * 민감한 정보가 포함된 메소드 - 파라미터 로깅 비활성화
     */
    @Auditable(
        action = "비밀번호 변경",
        category = "SECURITY",
        level = Auditable.AuditLevel.HIGH,
        logParameters = false,  // 비밀번호가 포함되므로 파라미터 로깅 비활성화
        logResult = false
    )
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("비밀번호 변경 요청: userId={}", userId);
        
        // 비밀번호 변경 로직 시뮬레이션
        simulateProcessingTime(300);
        
        // 실패 케이스 시뮬레이션 (10% 확률)
        if (ThreadLocalRandom.current().nextInt(100) < 10) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다");
        }
        
        log.info("비밀번호 변경 완료: userId={}", userId);
        return true;
    }

    /**
     * 예외 발생 시나리오 데모
     */
    @Auditable(
        action = "데이터 검증",
        category = "VALIDATION",
        level = Auditable.AuditLevel.MEDIUM
    )
    public void validateData(String data) {
        log.info("데이터 검증 시작: {}", data);
        
        simulateProcessingTime(150);
        
        // 검증 실패 시뮬레이션
        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("데이터가 비어있습니다");
        }
        
        if (data.length() > 100) {
            throw new IllegalArgumentException("데이터 길이가 제한을 초과했습니다");
        }
        
        log.info("데이터 검증 완료");
    }

    /**
     * Slow Query 시뮬레이션을 위한 Repository 호출 데모
     * 실제로는 Repository에서 발생하지만, 데모 목적으로 Service에서 시뮬레이션
     */
    public List<String> simulateSlowDatabaseQuery(String searchTerm) {
        log.info("데이터베이스 조회 시뮬레이션: {}", searchTerm);
        
        // 느린 쿼리 시뮬레이션 (1~3초)
        int delay = ThreadLocalRandom.current().nextInt(1000, 3000);
        simulateProcessingTime(delay);
        
        return List.of("결과1", "결과2", "결과3");
    }

    /**
     * 처리 시간 시뮬레이션 유틸리티 메소드
     */
    private void simulateProcessingTime(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("처리가 중단되었습니다", e);
        }
    }
}