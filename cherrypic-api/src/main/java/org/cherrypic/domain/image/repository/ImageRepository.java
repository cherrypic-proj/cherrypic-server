package org.cherrypic.domain.image.repository;

import java.util.List;
import org.cherrypic.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImageRepository extends JpaRepository<Image, Long>, ImageRepositoryCustom {
    List<Image> findAllById(Iterable<Long> imageIds);

    @Modifying(clearAutomatically = true)
    @Query(
            value =
                    """
      UPDATE image
         SET event_id   = :eventId,
             version    = version + 1,
             updated_at = CURRENT_TIMESTAMP(6)
       WHERE CONCAT(id, ':', version) IN (:keys)
         AND (event_id IS NULL OR event_id = :eventId)
    """,
            nativeQuery = true)
    int bulkChangeImageEventWithVersionCheck(
            @Param("keys") List<String> keys, @Param("eventId") Long eventId);
}
