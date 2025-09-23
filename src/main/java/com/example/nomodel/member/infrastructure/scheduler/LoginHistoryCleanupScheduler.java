package com.example.nomodel.member.infrastructure.scheduler;

import com.example.nomodel.member.domain.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginHistoryCleanupScheduler {

    private final LoginHistoryRepository loginHistoryRepository;

    /**
     * 오래된 로그인 내역 정리 (GDPR 준수)
     * 매일 새벽 3시에 실행
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldLoginHistory() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90); // 90일 이상 된 기록 삭제
        
        var oldRecords = loginHistoryRepository.findAll().stream()
                .filter(h -> h.getCreatedAt().isBefore(cutoffDate))
                .toList();
        
        if (!oldRecords.isEmpty()) {
            loginHistoryRepository.deleteAll(oldRecords);
            log.info("Deleted {} old login history records", oldRecords.size());
        }
    }
}