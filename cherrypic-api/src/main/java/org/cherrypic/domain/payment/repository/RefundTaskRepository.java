package org.cherrypic.domain.payment.repository;

import org.cherrypic.payment.entity.RefundTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundTaskRepository extends JpaRepository<RefundTask, Long> {}
