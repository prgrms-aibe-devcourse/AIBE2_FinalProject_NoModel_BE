package com.example.nomodel.member.application.service;

import com.example.nomodel.member.application.dto.response.UserInfoResponse;
import com.example.nomodel.member.domain.model.*;
import com.example.nomodel.member.domain.repository.FirstLoginRedisRepository;
import com.example.nomodel.member.domain.repository.LoginHistoryRepository;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.model.domain.repository.AIModelJpaRepository;
import com.example.nomodel.model.domain.repository.AdResultJpaRepository;
import com.example.nomodel.point.domain.model.MemberPointBalance;
import com.example.nomodel.point.domain.repository.MemberPointBalanceRepository;
import com.example.nomodel.subscription.domain.model.PlanType;
import com.example.nomodel.subscription.domain.model.SubscriptionStatus;
import com.example.nomodel.subscription.domain.repository.MemberSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserInfoService 단위 테스트")
class UserInfoServiceTest {

    @Mock
    private MemberJpaRepository memberRepository;
    @Mock
    private MemberSubscriptionRepository memberSubscriptionRepository;
    @Mock
    private MemberPointBalanceRepository memberPointBalanceRepository;
    @Mock
    private AIModelJpaRepository aiModelRepository;
    @Mock
    private AdResultJpaRepository adResultRepository;
    @Mock
    private FirstLoginRedisRepository firstLoginRedisRepository;
    @Mock
    private LoginHistoryRepository loginHistoryRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserInfoService userInfoService;

    private Member testMember;
    private final Long testMemberId = 1L;

    @BeforeEach
    void setUp() {
        userInfoService = new UserInfoService(
                memberRepository,
                memberSubscriptionRepository,
                memberPointBalanceRepository,
                aiModelRepository,
                adResultRepository,
                firstLoginRedisRepository,
                loginHistoryRepository
        );

        testMember = Member.createMember(
                "testUser",
                Email.of("test@example.com"),
                Password.encode("password123", passwordEncoder)
        );
        testMember.setId(testMemberId);
    }

    @Test
    @DisplayName("사용자 정보 조회 - 성공 (Redis 캐시 히트)")
    void getUserInfo_Success_WithRedisCache() {
        // given
        given(memberRepository.findById(testMemberId)).willReturn(Optional.of(testMember));
        given(memberSubscriptionRepository.findByMemberId(testMemberId)).willReturn(java.util.List.of());
        given(memberPointBalanceRepository.findById(testMemberId)).willReturn(Optional.empty());
        given(aiModelRepository.countByOwnerId(testMemberId)).willReturn(5L);
        given(adResultRepository.countByMemberId(testMemberId)).willReturn(3L);
        given(firstLoginRedisRepository.isFirstLogin(testMemberId)).willReturn(false);

        // when
        UserInfoResponse result = userInfoService.getUserInfo(testMemberId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(testMemberId);
        assertThat(result.name()).isEqualTo("testUser");
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.planType()).isEqualTo(PlanType.FREE.getValue());
        assertThat(result.points()).isEqualTo(0);
        assertThat(result.role()).isEqualTo("USER");
        assertThat(result.modelCount()).isEqualTo(5L);
        assertThat(result.projectCount()).isEqualTo(3L);
        assertThat(result.isFirstLogin()).isFalse();

        verify(firstLoginRedisRepository).isFirstLogin(testMemberId);
        verify(loginHistoryRepository, never()).existsByMemberIdAndLoginStatus(any(), any());
    }

    @Test
    @DisplayName("사용자 정보 조회 - 최초 로그인 true (Redis 캐시 히트)")
    void getUserInfo_FirstLoginTrue_WithRedisCache() {
        // given
        given(memberRepository.findById(testMemberId)).willReturn(Optional.of(testMember));
        given(memberSubscriptionRepository.findByMemberId(testMemberId)).willReturn(java.util.List.of());
        given(memberPointBalanceRepository.findById(testMemberId)).willReturn(Optional.empty());
        given(aiModelRepository.countByOwnerId(testMemberId)).willReturn(0L);
        given(adResultRepository.countByMemberId(testMemberId)).willReturn(0L);
        given(firstLoginRedisRepository.isFirstLogin(testMemberId)).willReturn(true);

        // when
        UserInfoResponse result = userInfoService.getUserInfo(testMemberId);

        // then
        assertThat(result.isFirstLogin()).isTrue();
        verify(firstLoginRedisRepository).isFirstLogin(testMemberId);
        verify(loginHistoryRepository, never()).existsByMemberIdAndLoginStatus(any(), any());
    }

    @Test
    @DisplayName("사용자 정보 조회 - 캐시 미스, DB에서 최초 로그인 확인 (최초 로그인)")
    void getUserInfo_CacheMiss_FirstLoginFromDB() {
        // given
        given(memberRepository.findById(testMemberId)).willReturn(Optional.of(testMember));
        given(memberSubscriptionRepository.findByMemberId(testMemberId)).willReturn(java.util.List.of());
        given(memberPointBalanceRepository.findById(testMemberId)).willReturn(Optional.empty());
        given(aiModelRepository.countByOwnerId(testMemberId)).willReturn(0L);
        given(adResultRepository.countByMemberId(testMemberId)).willReturn(0L);
        given(firstLoginRedisRepository.isFirstLogin(testMemberId)).willReturn(null);
        given(loginHistoryRepository.existsByMemberIdAndLoginStatus(testMemberId, LoginStatus.SUCCESS)).willReturn(false);

        // when
        UserInfoResponse result = userInfoService.getUserInfo(testMemberId);

        // then
        assertThat(result.isFirstLogin()).isTrue();
        verify(firstLoginRedisRepository).isFirstLogin(testMemberId);
        verify(loginHistoryRepository).existsByMemberIdAndLoginStatus(testMemberId, LoginStatus.SUCCESS);
        verify(firstLoginRedisRepository).setFirstLoginStatus(testMemberId, false);
    }

    @Test
    @DisplayName("사용자 정보 조회 - 캐시 미스, DB에서 최초 로그인 확인 (최초 로그인 아님)")
    void getUserInfo_CacheMiss_NotFirstLoginFromDB() {
        // given
        given(memberRepository.findById(testMemberId)).willReturn(Optional.of(testMember));
        given(memberSubscriptionRepository.findByMemberId(testMemberId)).willReturn(java.util.List.of());
        given(memberPointBalanceRepository.findById(testMemberId)).willReturn(Optional.empty());
        given(aiModelRepository.countByOwnerId(testMemberId)).willReturn(2L);
        given(adResultRepository.countByMemberId(testMemberId)).willReturn(1L);
        given(firstLoginRedisRepository.isFirstLogin(testMemberId)).willReturn(null);
        given(loginHistoryRepository.existsByMemberIdAndLoginStatus(testMemberId, LoginStatus.SUCCESS)).willReturn(true);

        // when
        UserInfoResponse result = userInfoService.getUserInfo(testMemberId);

        // then
        assertThat(result.isFirstLogin()).isFalse();
        verify(firstLoginRedisRepository).isFirstLogin(testMemberId);
        verify(loginHistoryRepository).existsByMemberIdAndLoginStatus(testMemberId, LoginStatus.SUCCESS);
        verify(firstLoginRedisRepository).setFirstLoginStatus(testMemberId, false);
    }

    @Test
    @DisplayName("사용자 정보 조회 - 포인트 잔액 있음")
    void getUserInfo_WithPointBalance() {
        // given
        MemberPointBalance pointBalance = new MemberPointBalance(testMemberId, BigDecimal.valueOf(1500));

        given(memberRepository.findById(testMemberId)).willReturn(Optional.of(testMember));
        given(memberSubscriptionRepository.findByMemberId(testMemberId)).willReturn(java.util.List.of());
        given(memberPointBalanceRepository.findById(testMemberId)).willReturn(Optional.of(pointBalance));
        given(aiModelRepository.countByOwnerId(testMemberId)).willReturn(0L);
        given(adResultRepository.countByMemberId(testMemberId)).willReturn(0L);
        given(firstLoginRedisRepository.isFirstLogin(testMemberId)).willReturn(false);

        // when
        UserInfoResponse result = userInfoService.getUserInfo(testMemberId);

        // then
        assertThat(result.points()).isEqualTo(1500);
    }

    @Test
    @DisplayName("사용자 정보 조회 - 존재하지 않는 회원")
    void getUserInfo_MemberNotFound() {
        // given
        given(memberRepository.findById(testMemberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userInfoService.getUserInfo(testMemberId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 회원입니다.");
    }
}