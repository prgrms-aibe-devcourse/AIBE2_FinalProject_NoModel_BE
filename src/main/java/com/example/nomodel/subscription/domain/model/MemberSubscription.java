package com.example.nomodel.subscription.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_subscription")
public class MemberSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    @ManyToOne
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    private Boolean autoRenewal;

    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime cancelledAt;

    @Enumerated(EnumType.STRING)
    private CancellationReason cancellationReason;

    private BigDecimal paidAmount;
    private Long paymentMethodId;

    protected MemberSubscription() {}

    public MemberSubscription(Long memberId, Subscription subscription, BigDecimal paidAmount) {
        this.memberId = memberId;
        this.subscription = subscription;
        this.status = SubscriptionStatus.ACTIVE;
        this.autoRenewal = true;
        this.startedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusDays(subscription.getPeriod());
        this.paidAmount = paidAmount;
    }

    // getter
    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public Subscription getSubscription() { return subscription; }
    public SubscriptionStatus getStatus() { return status; }
    public Boolean getAutoRenewal() { return autoRenewal; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public CancellationReason getCancellationReason() { return cancellationReason; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public Long getPaymentMethodId() { return paymentMethodId; }

    // 비즈니스 로직
    public void cancel(CancellationReason reason) {
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
    }
}
