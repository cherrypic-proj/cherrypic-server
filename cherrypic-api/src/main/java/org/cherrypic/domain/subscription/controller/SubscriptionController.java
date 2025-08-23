package org.cherrypic.domain.subscription.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.album.dto.response.*;
import org.cherrypic.domain.subscription.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/albums/{albumId}/subscriptions")
@RequiredArgsConstructor
@Tag(name = "8. 구독 API", description = "구독 관련 API입니다.")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @Operation(summary = "구독 해지", description = "유료 앨범의 구독을 해지합니다.")
    public ResponseEntity<Void> subscriptionCancel(@PathVariable Long albumId) {
        subscriptionService.cancelSubscription(albumId);
        return ResponseEntity.noContent().build();
    }
}
