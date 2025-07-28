package org.cherrypic.album.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.common.model.BaseTimeEntity;
import org.cherrypic.event.entity.Event;
import org.cherrypic.favorites.entity.Favorites;
import org.cherrypic.image.entity.Image;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.subscription.entity.Subscription;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Album extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull private String title;

    private String coverUrl;

    @Enumerated(EnumType.STRING)
    private AlbumPlan plan;

    @OneToOne(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private Subscription subscription;

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Event> events = new ArrayList<>();

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorites> favorites = new ArrayList<>();

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Album(String title, String coverUrl) {
        this.title = title;
        this.coverUrl = coverUrl;
    }

    public static Album createAlbum(String title, String coverUrl) {
        return Album.builder().title(title).coverUrl(coverUrl).build();
    }

    public void addParticipant(Participant participant) {
        participants.add(participant);
    }
}
