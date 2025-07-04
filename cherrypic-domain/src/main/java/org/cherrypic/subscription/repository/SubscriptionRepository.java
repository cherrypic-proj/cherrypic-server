package org.cherrypic.subscription.repository;

import org.cherrypic.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Long, Subscription> {}
