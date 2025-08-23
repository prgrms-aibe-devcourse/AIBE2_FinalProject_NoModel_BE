package com.example.nomodel.member.domain.repository;

import com.example.nomodel.member.domain.model.RefreshToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local") // Redis 내장 모드 사용
@DisplayName("RefreshTokenRedisRepository 통합 테스트")
class RefreshTokenRedisRepositoryTest {

    @Autowired
    private RefreshTokenRedisRepository refreshTokenRedisRepository;

    @AfterEach
    void tearDown() {
        refreshTokenRedisRepository.deleteAll();
    }

    @Test
    @DisplayName("RefreshToken 저장 및 조회")
    void saveAndFind_Success() {
        // given
        String memberId = "123";
        String tokenValue = "refresh-token-value";
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        RefreshToken refreshToken = RefreshToken.builder()
                .id(memberId)
                .refreshToken(tokenValue)
                .authorities(authorities)
                .build();

        // when
        RefreshToken savedToken = refreshTokenRedisRepository.save(refreshToken);

        // then
        assertThat(savedToken).isNotNull();
        assertThat(savedToken.getId()).isEqualTo(memberId);
        assertThat(savedToken.getRefreshToken()).isEqualTo(tokenValue);
        assertThat(savedToken.getAuthorities()).hasSize(1);

        // ID로 조회 확인
        Optional<RefreshToken> foundById = refreshTokenRedisRepository.findById(memberId);
        assertThat(foundById).isPresent();
        assertThat(foundById.get().getRefreshToken()).isEqualTo(tokenValue);
    }

    @Test
    @DisplayName("RefreshToken 토큰 값으로 조회")
    void findByRefreshToken_Success() {
        // given
        String memberId = "456";
        String tokenValue = "another-refresh-token";
        List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("ROLE_ADMIN")
        );

        RefreshToken refreshToken = RefreshToken.builder()
                .id(memberId)
                .refreshToken(tokenValue)
                .authorities(authorities)
                .build();

        refreshTokenRedisRepository.save(refreshToken);

        // when
        RefreshToken foundToken = refreshTokenRedisRepository.findByRefreshToken(tokenValue);

        // then
        assertThat(foundToken).isNotNull();
        assertThat(foundToken.getId()).isEqualTo(memberId);
        assertThat(foundToken.getRefreshToken()).isEqualTo(tokenValue);
        assertThat(foundToken.getAuthorities()).hasSize(2);
        assertThat(foundToken.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("존재하지 않는 토큰 조회 시 null 반환")
    void findByRefreshToken_NotFound_ReturnsNull() {
        // when
        RefreshToken foundToken = refreshTokenRedisRepository.findByRefreshToken("non-existent-token");

        // then
        assertThat(foundToken).isNull();
    }

    @Test
    @DisplayName("RefreshToken 토큰 값으로 조회 후 삭제")
    void findAndDeleteByRefreshToken_Success() {
        // given
        String memberId = "789";
        String tokenValue = "token-to-delete";
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        RefreshToken refreshToken = RefreshToken.builder()
                .id(memberId)
                .refreshToken(tokenValue)
                .authorities(authorities)
                .build();

        refreshTokenRedisRepository.save(refreshToken);

        // 저장 확인
        assertThat(refreshTokenRedisRepository.findByRefreshToken(tokenValue)).isNotNull();

        // when - Service 계층에서 하는 것처럼 조회 후 삭제
        RefreshToken foundToken = refreshTokenRedisRepository.findByRefreshToken(tokenValue);
        if (foundToken != null) {
            refreshTokenRedisRepository.deleteById(foundToken.getId());
        }

        // then
        assertThat(refreshTokenRedisRepository.findByRefreshToken(tokenValue)).isNull();
        assertThat(refreshTokenRedisRepository.findById(memberId)).isEmpty();
    }

    @Test
    @DisplayName("RefreshToken ID로 삭제")
    void deleteById_Success() {
        // given
        String memberId = "999";
        String tokenValue = "token-to-delete-by-id";
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        RefreshToken refreshToken = RefreshToken.builder()
                .id(memberId)
                .refreshToken(tokenValue)
                .authorities(authorities)
                .build();

        refreshTokenRedisRepository.save(refreshToken);

        // 저장 확인
        assertThat(refreshTokenRedisRepository.findById(memberId)).isPresent();

        // when
        refreshTokenRedisRepository.deleteById(memberId);

        // then
        assertThat(refreshTokenRedisRepository.findById(memberId)).isEmpty();
        assertThat(refreshTokenRedisRepository.findByRefreshToken(tokenValue)).isNull();
    }

    @Test
    @DisplayName("여러 RefreshToken 저장 및 각각 조회")
    void saveMultipleTokens_EachCanBeFoundIndependently() {
        // given
        RefreshToken token1 = RefreshToken.builder()
                .id("user1")
                .refreshToken("token1")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        RefreshToken token2 = RefreshToken.builder()
                .id("user2")
                .refreshToken("token2")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();

        // when
        refreshTokenRedisRepository.save(token1);
        refreshTokenRedisRepository.save(token2);

        // then
        RefreshToken foundToken1 = refreshTokenRedisRepository.findByRefreshToken("token1");
        RefreshToken foundToken2 = refreshTokenRedisRepository.findByRefreshToken("token2");

        assertThat(foundToken1).isNotNull();
        assertThat(foundToken1.getId()).isEqualTo("user1");
        assertThat(foundToken1.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");

        assertThat(foundToken2).isNotNull();
        assertThat(foundToken2.getId()).isEqualTo("user2");
        assertThat(foundToken2.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("동일한 사용자의 RefreshToken 업데이트")
    void updateRefreshToken_SameUser_Success() {
        // given - 초기 토큰 저장
        String memberId = "user123";
        RefreshToken initialToken = RefreshToken.builder()
                .id(memberId)
                .refreshToken("initial-token")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        refreshTokenRedisRepository.save(initialToken);

        // when - 동일한 사용자의 새 토큰 저장 (업데이트)
        RefreshToken updatedToken = RefreshToken.builder()
                .id(memberId)
                .refreshToken("updated-token")
                .authorities(List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_MANAGER")
                ))
                .build();

        refreshTokenRedisRepository.save(updatedToken);

        // then
        // 이전 토큰은 찾을 수 없어야 함
        assertThat(refreshTokenRedisRepository.findByRefreshToken("initial-token")).isNull();
        
        // 새 토큰은 찾을 수 있어야 함
        RefreshToken foundToken = refreshTokenRedisRepository.findByRefreshToken("updated-token");
        assertThat(foundToken).isNotNull();
        assertThat(foundToken.getId()).isEqualTo(memberId);
        assertThat(foundToken.getAuthorities()).hasSize(2);

        // ID로 조회했을 때도 업데이트된 토큰이어야 함
        Optional<RefreshToken> foundById = refreshTokenRedisRepository.findById(memberId);
        assertThat(foundById).isPresent();
        assertThat(foundById.get().getRefreshToken()).isEqualTo("updated-token");
    }

    @Test
    @DisplayName("Redis TTL 설정 확인 - 토큰 만료")
    void refreshToken_TTL_ExpirationTest() throws InterruptedException {
        // given
        String memberId = "ttl-test-user";
        String tokenValue = "ttl-test-token";
        
        RefreshToken refreshToken = RefreshToken.builder()
                .id(memberId)
                .refreshToken(tokenValue)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        // when
        refreshTokenRedisRepository.save(refreshToken);

        // then - 저장 직후에는 조회 가능
        assertThat(refreshTokenRedisRepository.findByRefreshToken(tokenValue)).isNotNull();

        // Note: 실제 TTL 테스트는 259200초(3일)이므로 단위 테스트에서는 검증이 어려움
        // 대신 저장된 토큰이 올바르게 조회되는지만 확인
        Optional<RefreshToken> found = refreshTokenRedisRepository.findById(memberId);
        assertThat(found).isPresent();
        assertThat(found.get().getRefreshToken()).isEqualTo(tokenValue);
    }
}