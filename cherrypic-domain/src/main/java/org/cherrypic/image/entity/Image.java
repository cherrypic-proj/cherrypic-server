package org.cherrypic.image.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.common.model.BaseTimeEntity;
import org.cherrypic.event.entity.Event;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private Album album;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @NotNull private Long memberId;

    @NotNull private String url;

    private LocalDateTime generatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Builder(access = AccessLevel.PRIVATE)
    private Image(Album album, Event event, Long memberId, String url, LocalDateTime generatedAt) {
        this.album = album;
        this.event = event;
        this.memberId = memberId;
        this.url = url;
        this.generatedAt = generatedAt;
    }

    public static Image createImage(
            Album album, Event event, Long memberId, String url, LocalDateTime generatedAt) {
        return Image.builder()
                .album(album)
                .event(event)
                .memberId(memberId)
                .url(url)
                .generatedAt(generatedAt)
                .build();
    }
}
