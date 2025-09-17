package org.cherrypic.domain.refundtask.repository;

import java.util.List;
import java.util.Optional;
import org.cherrypic.refundtask.entity.RefundTask;
import org.cherrypic.refundtask.enums.RefundTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundTaskRepository extends JpaRepository<RefundTask, Long> {
    Optional<RefundTask> findByPaymentId(Long paymentId);

    List<RefundTask> findAllByStatus(RefundTaskStatus status);
}
