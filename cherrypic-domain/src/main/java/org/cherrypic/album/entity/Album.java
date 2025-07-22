package org.cherrypic.album.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.common.model.BaseTimeEntity;
import org.cherrypic.event.entity.Event;
import org.cherrypic.favorites.entity.Favorites;
import org.cherrypic.image.entity.Image;
import org.cherrypic.participant.entity.Participant;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Album extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull private String title;

    private String coverUrl;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AlbumType type;

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorites> favorites = new ArrayList<>();

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Event> events = new ArrayList<>();

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> participants = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Album(String title, String coverUrl, AlbumType type) {
        this.title = title;
        this.coverUrl = coverUrl;
        this.type = type;
    }

    public static Album createAlbum(String title, String coverUrl, AlbumType type) {
        return Album.builder().title(title).coverUrl(coverUrl).type(type).build();
    }

    public void addParticipant(Participant participant) {
        participants.add(participant);
    }
}
