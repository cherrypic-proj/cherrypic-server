package org.cherrypic.domain.subscription.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public void expireOverdueSubscriptions() {
        subscriptionRepository.bulkExpire(LocalDateTime.now());
    }
}
