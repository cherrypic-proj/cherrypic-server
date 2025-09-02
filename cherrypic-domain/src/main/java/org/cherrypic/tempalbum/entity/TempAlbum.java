package org.cherrypic.tempalbum.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.common.model.BaseTimeEntity;
import org.cherrypic.member.entity.Member;
import org.cherrypic.tempalbum.enums.TempAlbumType;

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

    @NotNull private BigDecimal capacityGb;

    @Enumerated(EnumType.STRING)
    private TempAlbumType type;

    @NotNull private LocalDateTime expiredAt;

    private String url;

    @OneToMany(mappedBy = "tempAlbum", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TempAlbumImage> tempAlbumImages = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private TempAlbum(
            Member member,
            String title,
            BigDecimal capacityGb,
            TempAlbumType type,
            LocalDateTime expiredAt) {
        this.member = member;
        this.title = title;
        this.capacityGb = capacityGb;
        this.type = type;
        this.expiredAt = expiredAt;
    }

    public static TempAlbum createTempAlbum(Member member, String title) {
        return TempAlbum.builder()
                .member(member)
                .title(title)
                .capacityGb(BigDecimal.ZERO)
                .type(TempAlbumType.DEFAULT)
                .expiredAt(LocalDateTime.now().plusDays(TempAlbumType.DEFAULT.getDaysToLive()))
                .build();
    }
}
