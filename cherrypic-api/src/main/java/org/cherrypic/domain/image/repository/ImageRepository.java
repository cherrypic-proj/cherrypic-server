package org.cherrypic.domain.image.repository;

import java.util.List;
import org.cherrypic.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImageRepository extends JpaRepository<Image, Long>, ImageRepositoryCustom {
    List<Image> findAllById(Iterable<Long> imageIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value =
                    """
            UPDATE image
               SET event_id = :eventId
             WHERE id IN (:imageIds)
            """,
            nativeQuery = true)
    void bulkChangeImageEvent(
            @Param("imageIds") List<Long> imageIds, @Param("eventId") Long eventId);
}
