package org.cherrypic.domain.payment.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.cherrypic.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long>, PaymentRepositoryCustom {
    Optional<Payment> findByMerchantUid(String merchantUid);

    Optional<Payment> findByImpUid(String impUid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :paymentId")
    Optional<Payment> findByIdWithPessimisticLock(Long paymentId);

    Optional<Payment> findTop1ByAlbumIdOrderByIdAsc(Long albumId);
}
