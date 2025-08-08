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
import org.cherrypic.notification.entity.Notification;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.payment.entity.Payment;
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

    @NotNull private Boolean permissionControl;

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    @OneToOne(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private Subscription subscription;

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Event> events = new ArrayList<>();

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorites> favorites = new ArrayList<>();

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Album(String title, String coverUrl, AlbumPlan plan, boolean permissionControl) {
        this.title = title;
        this.coverUrl = coverUrl;
        this.plan = plan;
        this.permissionControl = permissionControl;
    }

    public static Album createAlbum(
            String title, String coverUrl, AlbumPlan plan, boolean permissionControl) {
        return Album.builder()
                .title(title)
                .coverUrl(coverUrl)
                .plan(plan)
                .permissionControl(permissionControl)
                .build();
    }

    public void addParticipant(Participant participant) {
        participants.add(participant);
    }

    public void updateAlbum(String title, String coverUrl) {
        this.title = title;
        this.coverUrl = coverUrl;
    }

    public void togglePermissionControl() {
        this.permissionControl = !this.permissionControl;
    }
}
