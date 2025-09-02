package com.example.nomodel.subscription.domain.repository;

import com.example.nomodel.subscription.domain.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
}
