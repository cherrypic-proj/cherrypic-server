package org.cherrypic.member.entity;

import jakarta.persistence.*;
import lombok.*;
import org.cherrypic.common.model.BaseTimeEntity;
import org.cherrypic.member.enums.MemberRole;
import org.cherrypic.member.enums.MemberStatus;
import org.cherrypic.member.enums.Subscription;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nickname;

    //    auth 관련 사항
    //    @Embedded
    //    private OauthInfo oauthInfo;

    private String profile;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    private boolean appAlarm;

    @Enumerated(EnumType.STRING)
    private Subscription subscription;
}
