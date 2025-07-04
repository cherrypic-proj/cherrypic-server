package org.cherrypic.member.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.cherrypic.common.model.BaseTimeEntity;
import org.cherrypic.favorites.entity.Favorites;
import org.cherrypic.member.enums.MemberRole;
import org.cherrypic.member.enums.MemberStatus;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.payment.entity.Payment;
import org.cherrypic.subscription.entity.Subscription;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY)
    private Subscription subscription;

    private String nickname;

    @Embedded private OauthInfo oauthInfo;

    private String profile;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    private boolean appAlarm;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorites> favorites = new ArrayList<>();
}
