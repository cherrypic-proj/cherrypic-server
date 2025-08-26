package org.cherrypic.domain.subscription.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.subscription.dto.request.SubscriptionRenewRequest;
import org.cherrypic.domain.subscription.dto.response.SubscriptionRenewResponse;
import org.cherrypic.domain.subscription.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/albums/{albumId}/subscriptions")
@RequiredArgsConstructor
@Tag(name = "8. 구독 API", description = "구독 관련 API입니다.")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @DeleteMapping
    @Operation(summary = "구독 해지", description = "유료 앨범의 구독을 해지합니다.")
    public ResponseEntity<Void> subscriptionCancel(@PathVariable Long albumId) {
        subscriptionService.cancelSubscription(albumId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/renew")
    @Operation(summary = "구독 갱신", description = "해지된 유료 앨범의 구독을 다시 갱신합니다.")
    public SubscriptionRenewResponse subscriptionRenew(
            @PathVariable Long albumId, @Valid @RequestBody SubscriptionRenewRequest request) {
        return subscriptionService.renewSubscription(albumId, request);
    }
}
