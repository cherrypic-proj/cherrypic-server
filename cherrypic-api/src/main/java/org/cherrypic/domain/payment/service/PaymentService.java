package org.cherrypic.domain.payment.service;

import org.cherrypic.domain.payment.dto.request.PaymentReadyRequest;
import org.cherrypic.domain.payment.dto.response.PaymentReadyResponse;
import org.cherrypic.domain.payment.dto.response.PaymentUnlinkedResponse;
import org.cherrypic.domain.payment.dto.response.PaymentVerificationResponse;

public interface PaymentService {
    PaymentReadyResponse preparePayment(PaymentReadyRequest request);

    PaymentVerificationResponse verifyPayment(String impUid);

    PaymentUnlinkedResponse getUnlinkedPayment();
}
