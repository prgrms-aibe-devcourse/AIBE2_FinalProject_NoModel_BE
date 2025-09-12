package com.example.nomodel.subscription.application.scheduler;

import com.example.nomodel.subscription.application.service.PortOnePaymentService;
import com.example.nomodel.subscription.domain.model.MemberSubscription;
import com.example.nomodel.subscription.domain.model.SubscriptionStatus;
import com.example.nomodel.subscription.domain.repository.MemberSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final MemberSubscriptionRepository memberSubscriptionRepository;
    private final PortOnePaymentService paymentService;

    // yml에서 주입받기
    @Value("${portone.kakao.subscription-channel-key}")
    private String kakaoChannelKey;

    @Value("${portone.toss.subscription-channel-key}")
    private String tossChannelKey;

    /**
     * 만료일이 지난 ACTIVE 구독 → 결제 재시도
     * 매 정시마다 실행
     */
    @Scheduled(cron = "0 0 * * * *")
    public void processRenewals() {
        List<MemberSubscription> expiringSubs =
                memberSubscriptionRepository.findByStatusAndExpiresAtBefore(
                        SubscriptionStatus.ACTIVE, LocalDateTime.now()
                );

        for (MemberSubscription sub : expiringSubs) {
            // TODO: 실제로는 sub.getPaymentMethodId() 로 kakao/toss 구분
            String channelKey = (sub.getPaymentMethodId() != null && sub.getPaymentMethodId() == 1)
                    ? kakaoChannelKey
                    : tossChannelKey;

            boolean success = paymentService.processRecurringPayment(
                    sub.getCustomerUid(),
                    sub.getSubscription().getPrice(),
                    channelKey
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
