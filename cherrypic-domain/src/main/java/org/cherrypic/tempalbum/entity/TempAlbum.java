package org.cherrypic.tempalbum.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.common.model.BaseTimeEntity;
import org.cherrypic.exception.CustomException;
import org.cherrypic.member.entity.Member;
import org.cherrypic.tempalbum.enums.TempAlbumType;
import org.cherrypic.tempalbum.exception.TempAlbumDomainErrorCode;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TempAlbum extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @NotNull private String title;

    @NotNull private BigDecimal capacityMb;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TempAlbumType type;

    @NotNull private LocalDate expiredAt;

    private String webUrl;

    @Builder(access = AccessLevel.PRIVATE)
    private TempAlbum(
            Member member,
            String title,
            BigDecimal capacityMb,
            TempAlbumType type,
            LocalDate expiredAt) {
        this.member = member;
        this.title = title;
        this.capacityMb = capacityMb;
        this.type = type;
        this.expiredAt = expiredAt;
    }

    public static TempAlbum createTempAlbum(Member member, String title) {
        return TempAlbum.builder()
                .member(member)
                .title(title)
                .capacityMb(BigDecimal.ZERO)
                .type(TempAlbumType.DEFAULT)
                .expiredAt(LocalDate.now().plusDays(TempAlbumType.DEFAULT.getDaysToLive()))
                .build();
    }

    public void increaseCapacity(BigDecimal decimal) {
        if (this.capacityMb.add(decimal).compareTo(BigDecimal.valueOf(9999.99)) > 0) {
            throw new CustomException(
                    TempAlbumDomainErrorCode.TEMP_ALBUM_CAPACITY_INCREASE_OVER_LIMIT);
        }
        this.capacityMb = capacityMb.add(decimal);
    }

    public void decreaseCapacity(BigDecimal decimal) {
        if (this.capacityMb.subtract(decimal).compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException(
                    TempAlbumDomainErrorCode.TEMP_ALBUM_CAPACITY_DECREASE_UNDER_ZERO);
        }
        this.capacityMb = capacityMb.subtract(decimal);
    }
}
