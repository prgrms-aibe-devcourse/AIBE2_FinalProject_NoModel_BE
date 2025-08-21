package com.example.nomodel.member.application.service;

import com.example.nomodel.member.application.dto.request.SignUpDto;
import com.example.nomodel.member.domain.model.Email;
import com.example.nomodel.member.domain.model.Member;
import com.example.nomodel.member.domain.model.Password;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.member.domain.service.MemberDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class MemberAuthService {

    private final MemberJpaRepository memberJPARepository;
    private final MemberDomainService memberDomainService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원 가입
     * Application Service의 역할:
     * - 트랜잭션 관리
     * - DTO → Domain 객체 변환
     * - Domain Service 호출을 통한 비즈니스 로직 실행
     * - 인프라스트럭처 조정
     * 
     * @param requestDto 회원 가입 요청 DTO
     */
    @Transactional
    public void signUp(SignUpDto requestDto) {
        // 1. DTO → Domain 객체 변환
        Email email = Email.of(requestDto.email());
        
        // 2. Domain Service를 통한 비즈니스 규칙 검증
        memberDomainService.validateEmailUniqueness(email);
        
        // 3. 인프라스트럭처 활용 (비밀번호 암호화)
        Password password = Password.encode(requestDto.password(), passwordEncoder);
        
        // 4. Domain 객체 생성 및 저장
        Member member = Member.createMember(requestDto.username(), email, password);
        memberJPARepository.save(member);
    }
}
