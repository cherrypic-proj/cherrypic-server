package org.cherrypic.domain.subscription.scheduler;

import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.subscription.job.SubscriptionExpireJob;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionExpireScheduler {

    private final SubscriptionExpireJob subscriptionExpireJob;

    @Scheduled(cron = "0 0 0 * * *")
    public void runSubscriptionExpireJob() {
        subscriptionExpireJob.run();
    }
}
