package org.cherrypic.favorites.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

    @NotNull private Boolean marked;

    @Builder(access = AccessLevel.PRIVATE)
    private Favorites(Participant participant, Boolean marked) {
        this.participant = participant;
        this.marked = marked;
    }

    public static Favorites createFavorites(Participant participant) {
        return Favorites.builder().participant(participant).marked(false).build();
    }
}
