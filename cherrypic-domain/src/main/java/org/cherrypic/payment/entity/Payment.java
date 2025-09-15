package org.cherrypic.payment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.common.model.BaseTimeEntity;
import org.cherrypic.exception.CustomException;
import org.cherrypic.member.entity.Member;
import org.cherrypic.payment.enums.PaymentPurpose;
import org.cherrypic.payment.enums.PaymentStatus;
import org.cherrypic.payment.exception.PaymentDomainErrorCode;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private Album album;

    @NotNull private String merchantUid;

    private String impUid;

    private String pgProvider;

    @NotNull private int amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentPurpose purpose;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AlbumType albumType;

    private LocalDateTime paidAt;

    private LocalDateTime canceledAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Payment(
            Member member,
            String merchantUid,
            int amount,
            PaymentStatus status,
            PaymentPurpose purpose,
            AlbumType albumType) {
        this.member = member;
        this.merchantUid = merchantUid;
        this.amount = amount;
        this.status = status;
        this.purpose = purpose;
        this.albumType = albumType;
    }

    public static Payment createPayment(
            Member member,
            String merchantUid,
            int amount,
            PaymentPurpose purpose,
            AlbumType albumType) {
        return Payment.builder()
                .member(member)
                .merchantUid(merchantUid)
                .amount(amount)
                .status(PaymentStatus.READY)
                .purpose(purpose)
                .albumType(albumType)
                .build();
    }

    public void complete(String impUid, String pgProvider, LocalDateTime paidAt) {
        this.impUid = impUid;
        this.pgProvider = pgProvider;
        this.status = PaymentStatus.PAID;
        this.paidAt = paidAt;
    }

    public void cancel(LocalDateTime canceledAt) {
        if (this.status == PaymentStatus.CANCELED) {
            throw new CustomException(PaymentDomainErrorCode.ALREADY_CANCELED);
        }
        if (this.status != PaymentStatus.PAID) {
            throw new CustomException(PaymentDomainErrorCode.ONLY_PAID_PAYMENT_CANCELABLE);
        }

        this.status = PaymentStatus.CANCELED;
        this.canceledAt = canceledAt;
    }

    public void assignToAlbum(PaymentPurpose purpose, Album album) {
        if (this.status == PaymentStatus.CANCELED) {
            throw new CustomException(PaymentDomainErrorCode.ALREADY_CANCELED);
        }
        if (this.status != PaymentStatus.PAID) {
            throw new CustomException(PaymentDomainErrorCode.NOT_PAID);
        }
        if (this.album != null) {
            throw new CustomException(PaymentDomainErrorCode.ALREADY_USED_PAYMENT);
        }
        if (this.purpose != purpose) {
            throw new CustomException(PaymentDomainErrorCode.PAYMENT_PURPOSE_MISMATCH);
        }

        this.album = album;
    }
}
