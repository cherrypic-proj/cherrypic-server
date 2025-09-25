package org.cherrypic.domain.subscription.repository;

import java.util.Optional;
import org.cherrypic.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByAlbumId(Long albumId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Subscription s where s.album.id = :albumId")
    void deleteByAlbumId(Long albumId);
}
