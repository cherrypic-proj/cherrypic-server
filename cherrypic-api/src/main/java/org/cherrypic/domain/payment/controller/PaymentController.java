package org.cherrypic.domain.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.payment.dto.request.PaymentReadyRequest;
import org.cherrypic.domain.payment.dto.response.PaymentListResponse;
import org.cherrypic.domain.payment.dto.response.PaymentReadyResponse;
import org.cherrypic.domain.payment.dto.response.PaymentUnlinkedResponse;
import org.cherrypic.domain.payment.dto.response.PaymentVerificationResponse;
import org.cherrypic.domain.payment.service.PaymentService;
import org.cherrypic.global.annotation.PageSize;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "5. 결제 API", description = "결제 관련 API입니다.")
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/ready")
    @Operation(
            summary = "유료 앨범 결제 준비",
            description = "유료 앨범 유형(PRO 또는 PREMIUM)에 대해 최초 생성, 구독 갱신, 구독 업그레이드의 상황에 맞게 결제를 준비합니다.")
    public PaymentReadyResponse paymentPrepare(@Valid @RequestBody PaymentReadyRequest request) {
        return paymentService.preparePayment(request);
    }

    @PostMapping("/verify/{impUid}")
    @Operation(
            summary = "impUid 기반 결제 검증",
            description = "impUid에 해당하는 결제 정보를 아임포트에서 조회하고, 결제 금액과 상태를 검증합니다.")
    public PaymentVerificationResponse paymentVerify(@PathVariable String impUid) {
        return paymentService.verifyPayment(impUid);
    }

    @PostMapping("/cancel/{impUid}")
    @Operation(summary = "impUid 기반 결제 취소", description = "impUid에 해당하는 결제를 전체 환불 처리합니다.")
    public ResponseEntity<Void> paymentCancel(@PathVariable String impUid) {
        paymentService.cancelPayment(impUid);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unlinked")
    @Operation(
            summary = "앨범과 연결되지 않은 완료된 결제 내역 조회",
            description = "결제는 완료되었지만 아직 앨범 생성, 구독 갱신, 또는 구독 업그레이드에 사용되지 않은 결제 내역을 조회합니다.")
    public PaymentUnlinkedResponse getUnlinkedPayment() {
        return paymentService.getUnlinkedPayment();
    }

    @GetMapping
    @Operation(summary = "앨범의 결제 내역 조회", description = "특정 앨범에 대한 결제 내역을 조회합니다.")
    public SliceResponse<PaymentListResponse> paymentsGet(
            @Parameter(description = "앨범 ID") @RequestParam Long albumId,
            @Parameter(description = "이전 페이지의 마지막 결제 ID (첫 요청 시 생략)")
                    @RequestParam(required = false)
                    Long lastPaymentId,
            @Parameter(description = "페이지당 조회할 결제 수") @RequestParam @PageSize Integer size,
            @Parameter(description = "정렬 방향 (ASC: 오래된순, DESC: 최신순)")
                    @RequestParam(defaultValue = "DESC")
                    SortDirection direction) {
        return paymentService.getAlbumPayments(albumId, lastPaymentId, size, direction);
    }
}
