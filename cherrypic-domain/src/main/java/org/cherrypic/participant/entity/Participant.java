package org.cherrypic.participant.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.common.model.BaseTimeEntity;
import org.cherrypic.favorites.entity.Favorites;
import org.cherrypic.member.entity.Member;
import org.cherrypic.participant.enums.ParticipantRole;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private Album album;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ParticipantRole role;

    @OneToOne(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true)
    private Favorites favorites;

    @Builder(access = AccessLevel.PRIVATE)
    private Participant(Member member, Album album, ParticipantRole role) {
        this.member = member;
        this.album = album;
        this.role = role;
    }

    public static Participant createParticipant(Member member, Album album, ParticipantRole role) {
        return Participant.builder().member(member).album(album).role(role).build();
    }

    public void assignFavorites() {
        this.favorites = Favorites.createFavorites(this);
    }

    public void changeRole(ParticipantRole role) {
        this.role = role;
    }
}
