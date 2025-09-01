package org.cherrypic.domain.payment.repository;

import java.util.Optional;
import org.cherrypic.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long>, PaymentRepositoryCustom {
    Optional<Payment> findByMerchantUid(String merchantUid);
}
