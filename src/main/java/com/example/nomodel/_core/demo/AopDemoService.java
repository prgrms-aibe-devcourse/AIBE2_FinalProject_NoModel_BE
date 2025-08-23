package com.example.nomodel._core.demo;

import com.example.nomodel._core.aop.annotation.BusinessCritical;
import com.example.nomodel._core.aop.annotation.BusinessCritical.BusinessDomain;
import com.example.nomodel._core.aop.annotation.BusinessCritical.CriticalLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @BusinessCritical 어노테이션 기반 AOP 기능 데모를 위한 서비스 클래스
 * - 일반적인 성능 메트릭은 Actuator가 자동 수집 (Prometheus/Grafana)
 * - 비즈니스 중요 로직만 ELK Stack용 구조화 로깅 적용
 * - 실제 프로젝트에서는 제거하고, 각각의 비즈니스 서비스에 @BusinessCritical 어노테이션 적용
 */
@Slf4j
@Service
@BusinessCritical(domain = BusinessDomain.GENERAL, level = CriticalLevel.LOW, description = "AOP 데모 서비스")
public class AopDemoService {

    /**
     * 비즈니스 특화 Service 모니터링 AOP 데모
     * - ServicePerformanceAspect가 비즈니스 중요 메소드만 선별 모니터링
     * - 임계값 초과하거나 비즈니스 중요 로직일 때만 ELK Stack에 구조화 로깅
     * - 일반적인 성능 메트릭은 Actuator → Prometheus → Grafana 경로로 처리
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
     * 사용자 정보 조회 - 비즈니스 중요 로직 데모
     * @BusinessCritical 어노테이션을 통한 비즈니스 로직 모니터링
     */
    @BusinessCritical(domain = BusinessDomain.USER, level = CriticalLevel.MEDIUM, description = "사용자 정보 조회")
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
     * 결제 처리 - 비즈니스 중요 로직 데모 (최고 중요도)
     */
    @BusinessCritical(domain = BusinessDomain.PAYMENT, level = CriticalLevel.CRITICAL, description = "결제 트랜잭션 처리")
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
     * 상품 목록 조회 - 일반 조회 로직 (클래스 레벨 어노테이션 적용)
     */
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
     * 비밀번호 변경 - 보안 관련 비즈니스 중요 로직
     */
    @BusinessCritical(domain = BusinessDomain.SECURITY, level = CriticalLevel.HIGH, description = "비밀번호 변경")
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
     * 데이터 검증 - 예외 발생 시나리오 데모 (클래스 레벨 어노테이션 적용)
     */
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
     * Slow Query 시뮬레이션 데모 - 비즈니스 중요 쿼리 감지
     * - SlowQueryDetectorAspect가 비즈니스 중요 쿼리나 임계값 초과 쿼리만 ELK 로깅
     * - 일반적인 DB 메트릭은 Actuator → Prometheus → Grafana 경로로 처리
     * - 실제로는 Repository에서 발생하지만, 데모 목적으로 Service에서 시뮬레이션
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