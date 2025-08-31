package org.cherrypic.domain.payment.repository;

import java.util.Optional;
import org.cherrypic.domain.payment.dto.response.PaymentUnlinkedResponse;

public interface PaymentRepositoryCustom {
    Optional<PaymentUnlinkedResponse> findLatestPaidUnlinkedPayment(Long memberId);
}
