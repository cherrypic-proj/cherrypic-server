package org.cherrypic.domain.subscription.service;

import org.cherrypic.domain.subscription.dto.request.SubscriptionRenewRequest;
import org.cherrypic.domain.subscription.dto.response.SubscriptionInfoResponse;
import org.cherrypic.domain.subscription.dto.response.SubscriptionRenewResponse;

public interface SubscriptionService {
    void cancelSubscription(Long albumId);

    SubscriptionRenewResponse renewSubscription(Long albumId, SubscriptionRenewRequest request);

    SubscriptionInfoResponse getSubscriptionInfo(Long albumId);
}
