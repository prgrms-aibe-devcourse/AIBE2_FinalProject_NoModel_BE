package com.example.nomodel.subscription.application.scheduler;

import com.example.nomodel.subscription.application.service.PortOnePaymentService;
import com.example.nomodel.subscription.domain.model.MemberSubscription;
import com.example.nomodel.subscription.domain.model.SubscriptionStatus;
import com.example.nomodel.subscription.domain.repository.MemberSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final MemberSubscriptionRepository memberSubscriptionRepository;
    private final PortOnePaymentService paymentService;

    /**
     * 만료일이 지난 ACTIVE 구독 → 카카오 결제 재시도
     * 매 정시마다 실행
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void processRenewals() {
        List<MemberSubscription> expiringSubs =
                memberSubscriptionRepository.findByStatusAndExpiresAtBefore(
                        SubscriptionStatus.ACTIVE, LocalDateTime.now()
                );

        for (MemberSubscription sub : expiringSubs) {
            boolean success = paymentService.processKakaoRecurring(
                    sub.getCustomerUid(),
                    sub.getSubscription().getPrice()
            );

            if (success) {
                sub.extend();
            } else {
                sub.markPastDue();
            }
            memberSubscriptionRepository.save(sub);
        }
    }

    /**
     * PAST_DUE 상태에서 3일 이상 지나면 → EXPIRED 전환
     * 매일 새벽 3시 실행
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void processPastDueToExpired() {
        List<MemberSubscription> pastDueSubs =
                memberSubscriptionRepository.findByStatusAndExpiresAtBefore(
                        SubscriptionStatus.PAST_DUE, LocalDateTime.now().minusDays(3)
                );

        for (MemberSubscription sub : pastDueSubs) {
            sub.expire();
            memberSubscriptionRepository.save(sub);
        }
    }
}
