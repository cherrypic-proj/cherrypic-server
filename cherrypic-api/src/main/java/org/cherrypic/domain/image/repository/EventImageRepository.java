package org.cherrypic.domain.image.repository;

import java.util.List;
import org.cherrypic.event.entity.EventImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventImageRepository extends JpaRepository<EventImage, Long> {
    long countByEventId(Long eventId);

    List<EventImage> findByEventIdAndImageIdInOrderByImageId(Long eventId, List<Long> imageIds);
}
