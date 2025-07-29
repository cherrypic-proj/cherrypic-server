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
import org.cherrypic.member.entity.Member;
import org.cherrypic.payment.enums.PaymentStatus;

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

    private LocalDateTime paidAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Payment(Member member, String merchantUid, int amount, PaymentStatus status) {
        this.member = member;
        this.merchantUid = merchantUid;
        this.amount = amount;
        this.status = status;
    }

    public static Payment createPayment(Member member, String merchantUid, int amount) {
        return Payment.builder()
                .member(member)
                .merchantUid(merchantUid)
                .amount(amount)
                .status(PaymentStatus.READY)
                .build();
    }

    public void updatePayment(
            String impUid, String pgProvider, PaymentStatus status, LocalDateTime paidAt) {
        this.impUid = impUid;
        this.pgProvider = pgProvider;
        this.status = status;
        this.paidAt = paidAt;
    }
}
