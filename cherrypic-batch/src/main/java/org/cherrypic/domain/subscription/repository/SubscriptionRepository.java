package org.cherrypic.domain.subscription.repository;

import java.time.LocalDateTime;
import org.cherrypic.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    @Modifying(clearAutomatically = true)
    @Query(
            value =
                    "update subscription set status = 'EXPIRED', next_billing_at = null where status IN ('ACTIVE', 'CANCELED') and end_at < :now",
            nativeQuery = true)
    void bulkExpire(LocalDateTime now);
}
