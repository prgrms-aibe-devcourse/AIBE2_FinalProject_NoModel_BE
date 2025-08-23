package com.example.nomodel._core.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 비즈니스 중요 API를 표시하는 어노테이션
 * 이 어노테이션이 적용된 컨트롤러나 메소드는 ELK Stack 상세 모니터링 대상이 됨
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessCritical {
    
    /**
     * 비즈니스 도메인 카테고리
     */
    BusinessDomain domain() default BusinessDomain.GENERAL;
    
    /**
     * 중요도 레벨
     */
    CriticalLevel level() default CriticalLevel.MEDIUM;
    
    /**
     * 설명 (선택사항)
     */
    String description() default "";
    
    /**
     * 비즈니스 도메인 열거형
     */
    enum BusinessDomain {
        AUTH("인증/인가"),
        PAYMENT("결제"),
        ORDER("주문"),
        USER("사용자"),
        MEMBER("회원"),
        SECURITY("보안"),
        FINANCIAL("금융"),
        GENERAL("일반");
        
        private final String description;
        
        BusinessDomain(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 중요도 레벨
     */
    enum CriticalLevel {
        LOW("낮음"),
        MEDIUM("중간"),
        HIGH("높음"),
        CRITICAL("매우 중요");
        
        private final String description;
        
        CriticalLevel(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}