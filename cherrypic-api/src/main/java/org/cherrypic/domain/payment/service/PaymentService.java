package org.cherrypic.domain.payment.service;

import org.cherrypic.domain.payment.dto.PaymentReadyRequest;
import org.cherrypic.domain.payment.dto.PaymentReadyResponse;

public interface PaymentService {
    PaymentReadyResponse preparePayment(PaymentReadyRequest request);

    void verifyPayment(String impUid);
}
