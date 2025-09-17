package org.cherrypic.domain.payment.event;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.payment.dto.RefundTaskDto;
import org.cherrypic.domain.payment.dto.event.PaymentVerifyEvent;
import org.cherrypic.domain.payment.service.PaymentAutoRefundService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RefundScheduleEventListener {

    private final TaskScheduler taskScheduler;
    private final PaymentAutoRefundService paymentAutoRefundService;

    @EventListener(ApplicationReadyEvent.class)
    public void initRefundTaskSchedule() {
        List<RefundTaskDto> pendingTasks = paymentAutoRefundService.findAllPending();
        pendingTasks.forEach(this::addSchedule);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void addRefundTaskSchedule(PaymentVerifyEvent event) {
        RefundTaskDto refundTask =
                paymentAutoRefundService.createRefundTask(event.paymentId(), event.paidAt());
        addSchedule(refundTask);
    }

    private void addSchedule(RefundTaskDto refundTask) {
        Long paymentId = refundTask.paymentId();
        Instant scheduledAt = refundTask.scheduledAt().atZone(ZoneId.systemDefault()).toInstant();
        taskScheduler.schedule(() -> paymentAutoRefundService.refund(paymentId), scheduledAt);
    }
}
