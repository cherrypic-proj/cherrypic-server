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
import org.cherrypic.domain.payment.repository.RefundTaskRepository;
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
    private final RefundTaskRepository refundTaskRepository;

    public void scheduleAutoRefund(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);

        if (payment == null || payment.getStatus() != PaymentStatus.PAID) {
            return;
        }

        long delaySeconds =
                Duration.between(LocalDateTime.now(), payment.getPaidAt().plusMinutes(10))
                        .getSeconds();
        delaySeconds = Math.max(0, delaySeconds);

        Runnable autoRefundTask =
                () ->
                        paymentRepository
                                .findByIdWithPessimisticLock(payment.getId())
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

                                                refundTaskRepository
                                                        .findByPaymentId(p.getId())
                                                        .ifPresent(
                                                                task -> {
                                                                    task.complete(
                                                                            LocalDateTime.now());
                                                                    refundTaskRepository.save(task);
                                                                });

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
