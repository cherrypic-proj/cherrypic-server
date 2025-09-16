package org.cherrypic.domain.payment.service;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.request.CancelData;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cherrypic.domain.payment.repository.PaymentRepository;
import org.cherrypic.payment.entity.Payment;
import org.cherrypic.payment.enums.PaymentStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAutoRefundService {

    private final TaskScheduler taskScheduler;

    private final IamportClient iamportClient;
    private final PaymentRepository paymentRepository;

    public void scheduleAutoRefund(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PAID) {
            return;
        }

        long delaySeconds =
                Duration.between(LocalDateTime.now(), payment.getPaidAt().plusMinutes(10))
                        .getSeconds();
        delaySeconds = Math.max(0, delaySeconds);

        Runnable autoRefundTask =
                () ->
                        paymentRepository
                                .findById(payment.getId())
                                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                                .filter(p -> p.getAlbum() == null)
                                .ifPresent(
                                        p -> {
                                            try {
                                                CancelData cancelData =
                                                        new CancelData(
                                                                p.getImpUid(),
                                                                true,
                                                                BigDecimal.valueOf(p.getAmount()));
                                                iamportClient.cancelPaymentByImpUid(cancelData);

                                                p.cancel(LocalDateTime.now());
                                                paymentRepository.save(p);

                                                log.info(
                                                        "Refund succeeded: paymentId={}, canceledAt={}, thread={}",
                                                        p.getId(),
                                                        p.getCanceledAt(),
                                                        Thread.currentThread().getName());
                                            } catch (Exception e) {
                                                log.error(
                                                        "Exception: code={}, message={}, paymentId={}, thread={}",
                                                        e.getClass().getSimpleName(),
                                                        e.getMessage(),
                                                        p.getId(),
                                                        Thread.currentThread().getName(),
                                                        e);
                                            }
                                        });

        taskScheduler.schedule(autoRefundTask, Instant.now().plusSeconds(delaySeconds));
    }
}
