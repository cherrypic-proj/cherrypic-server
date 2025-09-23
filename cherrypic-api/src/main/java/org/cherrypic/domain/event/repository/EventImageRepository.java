package org.cherrypic.domain.event.repository;

import java.util.List;
import org.cherrypic.event.entity.Event;
import org.cherrypic.event.entity.EventImage;
import org.cherrypic.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EventImageRepository extends JpaRepository<EventImage, Long> {
    @Modifying(clearAutomatically = true)
    @Query("delete from EventImage ei where ei.event in :events")
    void deleteAllByEvents(List<Event> events);

    @Modifying(clearAutomatically = true)
    @Query("delete from EventImage ei where ei.event = :event")
    void deleteAllByEvent(Event event);

    @Modifying
    @Query("delete from EventImage ei where ei.image in :images")
    void deleteAllByImages(List<Image> images);
}
