package org.cherrypic.subscription.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.YearMonth;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.common.model.BaseTimeEntity;
import org.cherrypic.exception.CustomException;
import org.cherrypic.member.entity.Member;
import org.cherrypic.subscription.enums.SubscriptionStatus;
import org.cherrypic.subscription.exception.SubscriptionDomainErrorCode;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseTimeEntity {

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

    @Builder(access = AccessLevel.PRIVATE)
    private Subscription(
            Member member,
            Album album,
            SubscriptionStatus status,
            LocalDateTime startAt,
            LocalDateTime endAt,
            LocalDateTime nextBillingAt) {
        this.member = member;
        this.album = album;
        this.status = status;
        this.startAt = startAt;
        this.endAt = endAt;
        this.nextBillingAt = nextBillingAt;
    }

    public static Subscription createSubscription(
            Member member, Album album, LocalDateTime paidAt) {
        return Subscription.builder()
                .member(member)
                .album(album)
                .status(SubscriptionStatus.ACTIVE)
                .startAt(paidAt)
                .endAt(adjustEndDate(paidAt))
                .nextBillingAt(adjustEndDate(paidAt).minusDays(3))
                .build();
    }

    public void cancel() {
        if (this.status == SubscriptionStatus.CANCELED) {
            throw new CustomException(SubscriptionDomainErrorCode.ALREADY_CANCELED);
        }
        if (this.endAt.isBefore(LocalDateTime.now())) {
            throw new CustomException(SubscriptionDomainErrorCode.ALREADY_EXPIRED);
        }

        this.status = SubscriptionStatus.CANCELED;
    }

    public void renew() {
        if (this.endAt.isBefore(LocalDateTime.now())) {
            throw new CustomException(SubscriptionDomainErrorCode.ALREADY_EXPIRED);
        }
        if (this.status == SubscriptionStatus.ACTIVE) {
            throw new CustomException(SubscriptionDomainErrorCode.ALREADY_ACTIVE);
        }

        this.status = SubscriptionStatus.ACTIVE;
        this.endAt = adjustEndDate(this.endAt);
        this.nextBillingAt = this.endAt.minusDays(3);
    }

    public static LocalDateTime adjustEndDate(LocalDateTime baseDate) {
        boolean isAtEndOfMonth = baseDate.getDayOfMonth() >= 28;

        if (isAtEndOfMonth) {
            YearMonth nextMonth = YearMonth.from(baseDate.plusMonths(1));
            return nextMonth.atEndOfMonth().atTime(baseDate.toLocalTime());
        }

        return baseDate.plusMonths(1);
    }
}
