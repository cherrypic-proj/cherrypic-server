package org.cherrypic.domain.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.payment.dto.PaymentReadyRequest;
import org.cherrypic.domain.payment.dto.PaymentReadyResponse;
import org.cherrypic.domain.payment.service.PaymentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "5. 결제 API", description = "결제 관련 API입니다.")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/ready")
    @Operation(
            summary = "앨범 구독 플랜 결제 준비",
            description = "사용자가 선택한 앨범 구독 플랜에 대해 결제 요청에 필요한 정보를 생성합니다.")
    public PaymentReadyResponse paymentPrepare(@Valid @RequestBody PaymentReadyRequest request) {
        return paymentService.preparePayment(request);
    }
}
