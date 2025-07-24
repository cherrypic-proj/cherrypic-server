package org.cherrypic.domain.payment.service;

import org.cherrypic.domain.payment.dto.request.PaymentReadyRequest;
import org.cherrypic.domain.payment.dto.response.PaymentReadyResponse;

public interface PaymentService {
    PaymentReadyResponse preparePayment(PaymentReadyRequest request);

    void verifyPayment(String impUid);
}
