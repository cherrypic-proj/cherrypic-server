package org.cherrypic.favorites.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.favorites.enums.FavoriteStatus;
import org.cherrypic.participant.entity.Participant;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorites {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    private Participant participant;

    @NotNull
    @Enumerated(EnumType.STRING)
    private FavoriteStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private Favorites(Participant participant, FavoriteStatus status) {
        this.participant = participant;
        this.status = status;
    }

    public static Favorites createFavorites(Participant participant) {
        return Favorites.builder().participant(participant).status(FavoriteStatus.EXCLUDED).build();
    }
}
