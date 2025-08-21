package com.example.nomodel.member.application.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel._core.security.jwt.JWTTokenProvider;
import com.example.nomodel.member.application.dto.request.LoginRequestDto;
import com.example.nomodel.member.application.dto.request.SignUpRequestDto;
import com.example.nomodel.member.application.dto.response.AuthTokenDTO;
import com.example.nomodel.member.domain.model.*;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.member.domain.repository.RefreshTokenRedisRepository;
import com.example.nomodel.member.domain.service.MemberDomainService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class MemberAuthService {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberDomainService memberDomainService;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTTokenProvider jwtTokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

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
    public void signUp(SignUpRequestDto requestDto) {
        // 1. DTO → Domain 객체 변환
        Email email = Email.of(requestDto.email());
        
        // 2. Domain Service를 통한 비즈니스 규칙 검증
        memberDomainService.validateEmailUniqueness(email);
        
        // 3. 인프라스트럭처 활용 (비밀번호 암호화)
        Password password = Password.encode(requestDto.password(), passwordEncoder);
        
        // 4. Domain 객체 생성 및 저장
        Member member = Member.createMember(requestDto.username(), email, password);
        memberJpaRepository.save(member);
    }

    /**
     * 로그인
     * 
     * @param requestDto 로그인 요청 DTO (이메일, 비밀번호)
     * @return JWT 토큰 정보 (액세스 토큰, 리프레시 토큰)
     */
    @Transactional
    public AuthTokenDTO login(LoginRequestDto requestDto) {
        // 1. 이메일로 회원 조회
        Email email = Email.of(requestDto.email());
        Member member = memberJpaRepository.findByEmail(email)
                .orElseThrow(() -> new ApplicationException(ErrorCode.MEMBER_NOT_FOUND));

        // 2. 비밀번호 검증
        if (!member.validatePassword(requestDto.password(), passwordEncoder)) {
            throw new ApplicationException(ErrorCode.INVALID_PASSWORD);
        }

        // 3. 회원 상태 확인
        if (!member.isActive()) {
            throw new ApplicationException(ErrorCode.MEMBER_NOT_ACTIVE);
        }

        // 4. 토큰 발급
        return generateAuthTokens(member, requestDto.email(), requestDto.password());
    }

    /**
     * JWT 토큰 생성 및 리프레시 토큰 저장
     * 
     * @param member 회원 엔티티
     * @param email 사용자 이메일
     * @param password 사용자 비밀번호
     * @return JWT 토큰 정보
     */
    private AuthTokenDTO generateAuthTokens(Member member, String email, String password) {
        // Spring Security 인증 처리
        UsernamePasswordAuthenticationToken authenticationToken = 
                new UsernamePasswordAuthenticationToken(email, password);
        Authentication authentication = authenticationManagerBuilder.getObject()
                .authenticate(authenticationToken);

        // JWT 토큰 생성
        AuthTokenDTO authTokenDTO = jwtTokenProvider.generateToken(authentication);

        // 리프레시 토큰을 Redis에 저장
        RefreshToken refreshToken = RefreshToken.builder()
                .id(String.valueOf(member.getId()))  // memberId를 ID로 사용
                .authorities(authentication.getAuthorities())
                .refreshToken(authTokenDTO.refreshToken())
                .build();
        refreshTokenRedisRepository.save(refreshToken);

        log.info("로그인 성공: email={}, memberId={}", email, member.getId());
        return authTokenDTO;
    }

    /**
     * 리프레시 토큰으로 액세스 토큰 재발급
     * 
     * @param request HTTP 요청 (Authorization 헤더에서 리프레시 토큰 추출)
     * @return 새로운 JWT 토큰 정보
     */
    @Transactional(readOnly = true)
    public AuthTokenDTO refreshToken(HttpServletRequest request) {
        // 1. Authorization 헤더에서 리프레시 토큰 추출
        String refreshToken = jwtTokenProvider.resolveToken(request);
        if (refreshToken == null) {
            throw new ApplicationException(ErrorCode.TOKEN_NOT_FOUND);
        }

        // 2. 리프레시 토큰 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new ApplicationException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. 리프레시 토큰 타입 확인
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new ApplicationException(ErrorCode.INVALID_TOKEN_TYPE);
        }

        // 4. Redis 에서 리프레시 토큰 조회
        RefreshToken storedToken = refreshTokenRedisRepository.findByRefreshToken(refreshToken);
        if (storedToken == null) {
            throw new ApplicationException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        // 5. 새로운 액세스 토큰 생성 (memberId를 직접 사용)
        Long memberId = Long.valueOf(storedToken.getId());
        log.info("토큰 재발급: memberId={}", memberId);
        return jwtTokenProvider.generateToken(String.valueOf(memberId), memberId, storedToken.getAuthorities());
    }

    /**
     * 로그아웃
     * 
     * @param request HTTP 요청 (Authorization 헤더에서 리프레시 토큰 추출)
     */
    @Transactional
    public void logout(HttpServletRequest request) {
        // 1. Authorization 헤더에서 리프레시 토큰 추출
        String refreshToken = jwtTokenProvider.resolveToken(request);
        if (refreshToken == null) {
            throw new ApplicationException(ErrorCode.TOKEN_NOT_FOUND);
        }

        // 2. 리프레시 토큰 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new ApplicationException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. 리프레시 토큰으로 직접 삭제
        refreshTokenRedisRepository.deleteByRefreshToken(refreshToken);
        
        // TODO: 선택사항 - 액세스 토큰을 블랙리스트에 추가하여 만료 전까지 사용 불가능하게 할 수 있음
        // blacklistTokenService.addToBlacklist(accessToken);
        
        log.info("로그아웃 성공: refreshToken 삭제 완료");
    }
}
