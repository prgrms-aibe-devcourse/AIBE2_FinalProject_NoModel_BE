package com.example.nomodel._core.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 비즈니스 로직 감사 대상 메소드를 표시하는 어노테이션
 * 이 어노테이션이 적용된 메소드는 실행 시 감사 로그가 기록됨
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    
    /**
     * 감사 액션 설명
     * 예: "회원 가입", "주문 생성", "결제 처리"
     */
    String action() default "";
    
    /**
     * 감사 카테고리
     * 예: "USER", "ORDER", "PAYMENT", "ADMIN"
     */
    String category() default "GENERAL";
    
    /**
     * 파라미터 로깅 여부
     * true: 메소드 파라미터를 로그에 포함
     * false: 파라미터 제외 (민감한 정보가 있는 경우)
     */
    boolean logParameters() default true;
    
    /**
     * 결과 로깅 여부
     * true: 메소드 반환값을 로그에 포함
     * false: 반환값 제외 (민감한 정보가 있는 경우)
     */
    boolean logResult() default true;
    
    /**
     * 중요도 레벨
     * HIGH: 핵심 비즈니스 로직 (결제, 인증 등)
     * MEDIUM: 일반 비즈니스 로직
     * LOW: 조회 등 단순 작업
     */
    AuditLevel level() default AuditLevel.MEDIUM;
    
    /**
     * 감사 레벨 열거형
     */
    enum AuditLevel {
        HIGH,    // 핵심 비즈니스 로직
        MEDIUM,  // 일반 비즈니스 로직
        LOW      // 단순 작업
    }
}