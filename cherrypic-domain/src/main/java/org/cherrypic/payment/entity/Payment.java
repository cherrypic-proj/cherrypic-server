package org.cherrypic.payment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.album.entity.Album;
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

    private LocalDateTime paidAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Payment(
            Member member,
            String merchantUid,
            int amount,
            PaymentStatus status,
            PaymentPurpose purpose) {
        this.member = member;
        this.merchantUid = merchantUid;
        this.amount = amount;
        this.status = status;
        this.purpose = purpose;
    }

    public static Payment createPayment(
            Member member, String merchantUid, int amount, PaymentPurpose purpose) {
        return Payment.builder()
                .member(member)
                .merchantUid(merchantUid)
                .amount(amount)
                .status(PaymentStatus.READY)
                .purpose(purpose)
                .build();
    }

    public void updatePayment(
            String impUid, String pgProvider, PaymentStatus status, LocalDateTime paidAt) {
        this.impUid = impUid;
        this.pgProvider = pgProvider;
        this.status = status;
        this.paidAt = paidAt;
    }

    public void updatePayment(PaymentPurpose purpose, Album album) {
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
