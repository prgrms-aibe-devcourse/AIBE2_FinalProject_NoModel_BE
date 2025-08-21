package com.example.nomodel.member.domain.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.member.domain.model.Email;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Member Domain Service
 * 도메인 비즈니스 로직을 담당합니다.
 * - 여러 aggregate 간의 비즈니스 로직
 * - 단일 Entity로는 표현하기 어려운 비즈니스 규칙
 * - 도메인 불변식 검증
 */
@Service
@RequiredArgsConstructor
public class MemberDomainService {

    private final MemberJpaRepository memberJpaRepository;

    /**
     * 이메일 중복 검사
     * 도메인 규칙: 동일한 이메일로는 회원가입할 수 없다
     * 
     * @param email 검증할 이메일
     * @throws ApplicationException 이메일이 이미 존재하는 경우
     */
    public void validateEmailUniqueness(Email email) {
        if (memberJpaRepository.existsByEmail(email)) {
            throw new ApplicationException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    /**
     * 회원 활성화 가능 여부 검증
     * 도메인 규칙: 특정 조건을 만족하는 회원만 활성화 가능
     * 
     * @param email 검증할 이메일
     * @return 활성화 가능 여부
     */
    public boolean canActivateMember(Email email) {
        // 추후 복잡한 비즈니스 로직 추가 가능
        // 예: 이메일 인증 완료, 약관 동의, 관리자 승인 등
        return memberJpaRepository.findByEmail(email).isPresent();
    }

    /**
     * 회원 탈퇴 가능 여부 검증
     * 도메인 규칙: 진행 중인 주문이 있거나 정산되지 않은 포인트가 있는 경우 탈퇴 불가
     * 
     * @param memberId 회원 ID
     * @return 탈퇴 가능 여부
     */
    public boolean canDeactivateMember(Long memberId) {
        // 추후 다른 도메인과의 연관성 검증 로직 추가
        // 예: 주문 상태 확인, 포인트 잔액 확인 등
        return true;
    }
}