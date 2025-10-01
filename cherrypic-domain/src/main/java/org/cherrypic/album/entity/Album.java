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
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.album.exception.AlbumDomainErrorCode;
import org.cherrypic.common.model.BaseTimeEntity;
import org.cherrypic.exception.CustomException;
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

    @NotNull private Boolean permissionControl;

    @NotNull private BigDecimal capacityMb;

    @OneToMany(mappedBy = "album", cascade = CascadeType.PERSIST)
    private List<Participant> participants = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Album(
            String title,
            String coverUrl,
            AlbumType type,
            boolean permissionControl,
            BigDecimal capacityMb) {
        this.title = title;
        this.coverUrl = coverUrl;
        this.type = type;
        this.permissionControl = permissionControl;
        this.capacityMb = capacityMb;
    }

    public static Album createAlbum(
            String title, String coverUrl, AlbumType type, boolean permissionControl) {
        return Album.builder()
                .title(title)
                .coverUrl(coverUrl)
                .type(type)
                .permissionControl(permissionControl)
                .capacityMb(BigDecimal.ZERO)
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
        if (this.capacityMb.add(decimal).compareTo(BigDecimal.valueOf(9999999.99)) > 0) {
            throw new CustomException(AlbumDomainErrorCode.ALBUM_CAPACITY_INCREASE_OVER_LIMIT);
        }
        this.capacityMb = capacityMb.add(decimal);
    }

    public void decreaseCapacity(BigDecimal decimal) {
        if (this.capacityMb.subtract(decimal).compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException(AlbumDomainErrorCode.ALBUM_CAPACITY_DECREASE_UNDER_ZERO);
        }
        this.capacityMb = capacityMb.subtract(decimal);
    }
}
