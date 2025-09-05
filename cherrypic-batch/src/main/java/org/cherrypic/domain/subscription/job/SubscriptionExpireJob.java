package org.cherrypic.domain.subscription.job;

import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.subscription.service.SubscriptionService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionExpireJob {

    private final SubscriptionService subscriptionService;

    public void run() {
        subscriptionService.expireOverdueSubscriptions();
    }
}
