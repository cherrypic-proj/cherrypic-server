package org.cherrypic.subscription.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.member.entity.Member;
import org.cherrypic.subscription.enums.SubscriptionStatus;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", unique = true)
    private Album album;

    @NotNull
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private LocalDateTime nextBillingAt;
}
