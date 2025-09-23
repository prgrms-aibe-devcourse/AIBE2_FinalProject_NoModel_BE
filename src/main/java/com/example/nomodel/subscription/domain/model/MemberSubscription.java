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

    @ManyToOne(fetch = FetchType.LAZY)
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

    //PortOne 정기 결제를 위한 고객 UID(빌링키 개념)
    private String customerUid;

    protected MemberSubscription() {}

    public MemberSubscription(Long memberId, Subscription subscription, BigDecimal paidAmount, String customerUid) {
        this.memberId = memberId;
        this.subscription = subscription;
        this.status = SubscriptionStatus.ACTIVE;
        this.autoRenewal = true;
        this.startedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusDays(subscription.getPeriod());
        this.paidAmount = paidAmount;
        this.customerUid = customerUid;
    }

    // getter
    public Long getId() {return id;}
    public Long getMemberId() {  return memberId;}
    public Subscription getSubscription() {return subscription;}
    public SubscriptionStatus getStatus() {return status;}
    public Boolean getAutoRenewal() {return autoRenewal;}
    public LocalDateTime getStartedAt() {return startedAt;}
    public LocalDateTime getExpiresAt() {return expiresAt;}
    public LocalDateTime getCancelledAt() {return cancelledAt;}
    public CancellationReason getCancellationReason() {return cancellationReason;}
    public BigDecimal getPaidAmount() {return paidAmount;}
    public Long getPaymentMethodId() {return paymentMethodId;}
    public String getCustomerUid() {return customerUid;}

    // 비즈니스 로직

    /**
     * 구독 취소
     */
    public void cancel(CancellationReason reason) {
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
    }

    /**
     * 구독 연장 (정기결제 성공 시)
     */
    public void extend() {
        this.expiresAt = this.expiresAt.plusDays(subscription.getPeriod());
        this.status = SubscriptionStatus.ACTIVE;
    }

    /**
     * 결제 실패 시 PAST_DUE 전환
     */
    public void markPastDue() {
        this.status = SubscriptionStatus.PAST_DUE;
    }

    /**
     * 최종 만료 처리
     */
    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
    }

}

