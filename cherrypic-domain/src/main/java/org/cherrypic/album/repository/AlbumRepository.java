package org.cherrypic.album.repository;

import org.cherrypic.album.entity.Album;
import org.cherrypic.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumRepository extends JpaRepository<Album, Long> {
    boolean existsByPayment(Payment payment);
}
