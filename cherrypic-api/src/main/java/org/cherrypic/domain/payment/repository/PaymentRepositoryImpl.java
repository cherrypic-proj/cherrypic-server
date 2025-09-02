package org.cherrypic.domain.payment.repository;

import static org.cherrypic.payment.entity.QPayment.payment;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.payment.dto.response.PaymentListResponse;
import org.cherrypic.domain.payment.dto.response.PaymentUnlinkedResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.payment.enums.PaymentStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<PaymentUnlinkedResponse> findLatestPaidUnlinkedPayment(Long memberId) {
        return Optional.ofNullable(
                queryFactory
                        .select(
                                Projections.constructor(
                                        PaymentUnlinkedResponse.class,
                                        payment.id,
                                        payment.albumType,
                                        payment.amount,
                                        payment.purpose,
                                        payment.paidAt))
                        .from(payment)
                        .where(
                                payment.member.id.eq(memberId),
                                payment.status.eq(PaymentStatus.PAID),
                                payment.album.isNull())
                        .orderBy(payment.paidAt.desc())
                        .fetchFirst());
    }

    @Override
    public Slice<PaymentListResponse> findAllByAlbumId(
            Long albumId, Long lastPaymentId, int size, SortDirection direction) {
        List<PaymentListResponse> results =
                queryFactory
                        .select(
                                Projections.constructor(
                                        PaymentListResponse.class,
                                        payment.id,
                                        payment.paidAt,
                                        payment.amount))
                        .from(payment)
                        .where(
                                payment.album.id.eq(albumId),
                                payment.status.eq(PaymentStatus.PAID),
                                lastPaymentIdCondition(lastPaymentId, direction))
                        .orderBy(
                                direction == SortDirection.DESC
                                        ? payment.id.desc()
                                        : payment.id.asc())
                        .limit(size + 1)
                        .fetch();

        return checkLastPage(size, results);
    }

    private BooleanExpression lastPaymentIdCondition(Long paymentId, SortDirection direction) {
        if (paymentId == null) {
            return null;
        }

        return direction == SortDirection.DESC
                ? payment.id.lt(paymentId)
                : payment.id.gt(paymentId);
    }

    private Slice<PaymentListResponse> checkLastPage(
            int pageSize, List<PaymentListResponse> results) {
        boolean hasNext = false;

        if (results.size() > pageSize) {
            hasNext = true;
            results.remove(pageSize);
        }

        return new SliceImpl<>(results, PageRequest.of(0, pageSize), hasNext);
    }
}
