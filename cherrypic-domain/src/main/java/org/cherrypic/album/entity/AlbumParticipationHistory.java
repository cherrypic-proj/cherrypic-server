package org.cherrypic.album.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.album.enums.ParticipationAction;
import org.cherrypic.common.model.BaseTimeEntity;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlbumParticipationHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull private Long memberId;

    @NotNull private String albumTitleSnapshot;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ParticipationAction action;

    @Builder(access = AccessLevel.PRIVATE)
    private AlbumParticipationHistory(
            Long memberId, String albumTitleSnapshot, ParticipationAction action) {
        this.memberId = memberId;
        this.albumTitleSnapshot = albumTitleSnapshot;
        this.action = action;
    }

    public static AlbumParticipationHistory createAlbumParticipationHistory(
            Long memberId, String albumTitleSnapshot, ParticipationAction action) {
        return AlbumParticipationHistory.builder()
                .memberId(memberId)
                .albumTitleSnapshot(albumTitleSnapshot)
                .action(action)
                .build();
    }
}
