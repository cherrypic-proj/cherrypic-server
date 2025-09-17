package org.cherrypic.domain.payment.service;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.request.CancelData;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cherrypic.domain.payment.dto.RefundTaskDto;
import org.cherrypic.domain.payment.repository.PaymentRepository;
import org.cherrypic.domain.payment.repository.RefundTaskRepository;
import org.cherrypic.payment.entity.RefundTask;
import org.cherrypic.payment.enums.PaymentStatus;
import org.cherrypic.payment.enums.RefundTaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentAutoRefundService {

    private final IamportClient iamportClient;
    private final PaymentRepository paymentRepository;
    private final RefundTaskRepository refundTaskRepository;

    public RefundTaskDto createRefundTask(Long paymentId, LocalDateTime paidAt) {
        RefundTask refundTask =
                refundTaskRepository.save(
                        RefundTask.createRefundTask(paymentId, paidAt.plusMinutes(10)));
        return RefundTaskDto.from(refundTask);
    }

    @Transactional(readOnly = true)
    public List<RefundTaskDto> findAllPending() {
        return refundTaskRepository.findAllByStatus(RefundTaskStatus.PENDING).stream()
                .map(task -> new RefundTaskDto(task.getPaymentId(), task.getScheduledAt()))
                .toList();
    }

    public void refund(Long paymentId) {
        paymentRepository
                .findByIdWithPessimisticLock(paymentId)
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
                                                    task.complete(LocalDateTime.now());
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
    }
}
