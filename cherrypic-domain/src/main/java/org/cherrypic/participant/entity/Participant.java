package org.cherrypic.participant.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.common.model.BaseTimeEntity;
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

    @Enumerated(EnumType.STRING)
    private ParticipantRole role;

    private String password;
}
