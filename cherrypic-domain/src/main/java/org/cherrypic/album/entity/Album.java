package org.cherrypic.album.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.common.exception.DomainErrorCode;
import org.cherrypic.common.model.BaseTimeEntity;
import org.cherrypic.event.entity.Event;
import org.cherrypic.exception.CustomException;
import org.cherrypic.image.entity.Image;
import org.cherrypic.notification.entity.Notification;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.payment.entity.Payment;
import org.cherrypic.subscription.entity.Subscription;
import org.cherrypic.tempalbum.TempAlbum;

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

    @NotNull private BigDecimal capacityGb;

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
    private List<Image> images = new ArrayList<>();

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TempAlbum> tempAlbumImages = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Album(
            String title,
            String coverUrl,
            AlbumPlan plan,
            boolean permissionControl,
            BigDecimal capacityGb) {
        this.title = title;
        this.coverUrl = coverUrl;
        this.plan = plan;
        this.permissionControl = permissionControl;
        this.capacityGb = capacityGb;
    }

    public static Album createAlbum(
            String title, String coverUrl, AlbumPlan plan, boolean permissionControl) {
        return Album.builder()
                .title(title)
                .coverUrl(coverUrl)
                .plan(plan)
                .permissionControl(permissionControl)
                .capacityGb(BigDecimal.ZERO)
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

    public void increaseCapacity(BigDecimal decimal) {
        if (this.capacityGb.add(decimal).compareTo(BigDecimal.valueOf(9999.99)) > 0) {
            throw new CustomException(DomainErrorCode.ALBUM_CAPACITY_INCREASE_OVER_LIMIT);
        }
        this.capacityGb = capacityGb.add(decimal);
    }

    public void decreaseCapacity(BigDecimal decimal) {
        if (this.capacityGb.subtract(decimal).compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException(DomainErrorCode.ALBUM_CAPACITY_DECREASE_UNDER_ZERO);
        }
        this.capacityGb = capacityGb.subtract(decimal);
    }
}
