package com.example.nomodel._core.utils;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel._core.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {
        // Utility class
    }

    /**
     * 현재 인증된 사용자의 Authentication 객체를 반환
     * @return Authentication 객체
     */
    public static Authentication getCurrentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApplicationException(ErrorCode.AUTHENTICATION_FAILED);
        }
        return authentication;
    }

    /**
     * 현재 인증된 사용자의 CustomUserDetails를 반환
     * @return CustomUserDetails 객체
     */
    public static CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = getCurrentAuthentication();
        
        if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new ApplicationException(ErrorCode.AUTHENTICATION_FAILED);
        }
        
        return (CustomUserDetails) authentication.getPrincipal();
    }

    /**
     * 현재 인증된 사용자의 memberId를 반환
     * @return 회원 ID
     */
    public static Long getCurrentMemberId() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        
        if (userDetails.getMemberId() == null) {
            throw new ApplicationException(ErrorCode.AUTHENTICATION_FAILED);
        }
        
        return userDetails.getMemberId();
    }

    /**
     * 현재 인증된 사용자의 이메일을 반환
     * @return 사용자 이메일
     */
    public static String getCurrentEmail() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails.getUsername(); // CustomUserDetails에서 getUsername()은 email을 반환
    }

    /**
     * 현재 사용자가 특정 권한을 가지고 있는지 확인
     * @param authority 확인할 권한
     * @return 권한 보유 여부
     */
    public static boolean hasAuthority(String authority) {
        Authentication authentication = getCurrentAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
    }

    /**
     * 현재 사용자가 ADMIN 권한을 가지고 있는지 확인
     * @return ADMIN 권한 보유 여부
     */
    public static boolean isAdmin() {
        return hasAuthority("ADMIN");
    }

    /**
     * 현재 사용자가 USER 권한을 가지고 있는지 확인
     * @return USER 권한 보유 여부
     */
    public static boolean isUser() {
        return hasAuthority("USER");
    }
}