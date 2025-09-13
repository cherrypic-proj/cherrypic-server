package org.cherrypic.member.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.cherrypic.common.model.BaseTimeEntity;
import org.cherrypic.member.enums.MemberRole;
import org.cherrypic.member.enums.MemberStatus;
import org.cherrypic.participant.entity.Participant;
import org.cherrypic.payment.entity.Payment;
import org.cherrypic.subscription.entity.Subscription;
import org.cherrypic.tempalbum.entity.TempAlbum;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded private OauthInfo oauthInfo;

    @NotNull private String nickname;

    private String profileImageUrl;

    @NotNull
    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @NotNull
    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    @NotNull private Boolean serviceAlarmAgree;

    @NotNull private Boolean marketingAgree;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subscription> subscriptions = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TempAlbum> tempAlbums = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Member(
            OauthInfo oauthInfo,
            String nickname,
            String profileImageUrl,
            MemberRole role,
            MemberStatus status,
            Boolean serviceAlarmAgree,
            Boolean marketingAgree) {
        this.oauthInfo = oauthInfo;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
        this.status = status;
        this.serviceAlarmAgree = serviceAlarmAgree;
        this.marketingAgree = marketingAgree;
    }

    public static Member createMember(
            OauthInfo oauthInfo, String nickname, String profileImageUrl) {
        return Member.builder()
                .oauthInfo(oauthInfo)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .role(MemberRole.USER)
                .status(MemberStatus.NORMAL)
                .serviceAlarmAgree(Boolean.FALSE)
                .marketingAgree(Boolean.FALSE)
                .build();
    }

    public void updateMember(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }
}
