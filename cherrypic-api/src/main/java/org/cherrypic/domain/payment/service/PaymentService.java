package org.cherrypic.domain.payment.service;

import org.cherrypic.domain.payment.dto.request.PaymentReadyRequest;
import org.cherrypic.domain.payment.dto.response.PaymentListResponse;
import org.cherrypic.domain.payment.dto.response.PaymentReadyResponse;
import org.cherrypic.domain.payment.dto.response.PaymentUnlinkedResponse;
import org.cherrypic.domain.payment.dto.response.PaymentVerificationResponse;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;

public interface PaymentService {
    PaymentReadyResponse preparePayment(PaymentReadyRequest request);

    PaymentVerificationResponse verifyPayment(String impUid);

    PaymentUnlinkedResponse getUnlinkedPayment();

    SliceResponse<PaymentListResponse> getAlbumPayments(
            Long albumId, Long lastPaymentId, int size, SortDirection direction);
}
