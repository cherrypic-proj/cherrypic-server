package org.cherrypic.payment.repository;

import org.cherrypic.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Long, Payment> {}
