package org.cherrypic.domain.payment.event;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.payment.repository.RefundTaskRepository;
import org.cherrypic.domain.payment.service.PaymentAutoRefundService;
import org.cherrypic.payment.entity.RefundTask;
import org.cherrypic.payment.enums.RefundTaskStatus;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundTaskInitializer {

    private final PaymentAutoRefundService paymentAutoRefundService;

    private final RefundTaskRepository refundTaskRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void initRefundTaskSchedule() {
        List<RefundTask> pendingTasks =
                refundTaskRepository.findAllByStatus(RefundTaskStatus.PENDING);

        pendingTasks.forEach(
                task -> paymentAutoRefundService.scheduleAutoRefund(task.getPaymentId()));
    }
}
