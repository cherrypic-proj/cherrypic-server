package org.cherrypic.domain.payment.repository;

import java.util.Optional;
import org.cherrypic.domain.payment.dto.response.PaymentListResponse;
import org.cherrypic.domain.payment.dto.response.PaymentUnlinkedResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.data.domain.Slice;

public interface PaymentRepositoryCustom {
    Optional<PaymentUnlinkedResponse> findLatestPaidUnlinkedPayment(Long memberId);

    Slice<PaymentListResponse> findAllByAlbumId(
            Long albumId, Long lastPaymentId, int size, SortDirection direction);
}
