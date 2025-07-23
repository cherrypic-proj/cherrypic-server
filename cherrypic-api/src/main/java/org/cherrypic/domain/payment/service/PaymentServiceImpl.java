package org.cherrypic.domain.payment.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.domain.payment.dto.PaymentReadyRequest;
import org.cherrypic.domain.payment.dto.PaymentReadyResponse;
import org.cherrypic.domain.payment.exception.PaymentErrorCode;
import org.cherrypic.domain.payment.exception.PaymentException;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.member.entity.Member;
import org.cherrypic.payment.entity.Payment;
import org.cherrypic.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final MemberUtil memberUtil;
    private final PaymentRepository paymentRepository;

    @Override
    public PaymentReadyResponse preparePayment(PaymentReadyRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();

        AlbumPlan plan = request.plan();
        if (plan.equals(AlbumPlan.BASIC)) {
            throw new PaymentException(PaymentErrorCode.UNSUPPORTED_PAYMENT_PLAN);
        }

        int price = plan.getPrice();
        String merchantUid = generateMerchantUid(currentMember.getId(), plan);
        String buyerName = currentMember.getNickname();

        Payment payment = Payment.createPayment(currentMember, merchantUid, price);
        paymentRepository.save(payment);

        return PaymentReadyResponse.of(plan, price, merchantUid, buyerName);
    }

    private String generateMerchantUid(Long memberId, AlbumPlan plan) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        return String.format("album_%s_%s_%d_%s", date, plan.name().toLowerCase(), memberId, uuid);
    }
}
