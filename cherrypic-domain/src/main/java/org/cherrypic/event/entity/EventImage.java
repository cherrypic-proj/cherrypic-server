package org.cherrypic.event.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.common.model.BaseTimeEntity;
import org.cherrypic.image.entity.Image;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private Image image;

    @Builder(access = AccessLevel.PRIVATE)
    private EventImage(Event event, Image image) {
        this.event = event;
        this.image = image;
    }

    public static EventImage createEventImage(Event event, Image image) {
        return EventImage.builder().event(event).image(image).build();
    }
}
