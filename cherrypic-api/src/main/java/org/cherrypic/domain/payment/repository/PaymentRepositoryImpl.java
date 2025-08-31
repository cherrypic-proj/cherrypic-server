package org.cherrypic.domain.payment.repository;

import static org.cherrypic.payment.entity.QPayment.payment;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.payment.dto.response.PaymentUnlinkedResponse;
import org.cherrypic.payment.enums.PaymentStatus;
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
}
