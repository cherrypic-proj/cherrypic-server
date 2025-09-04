package org.cherrypic.image.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.common.model.BaseTimeEntity;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private Album album;

    @NotNull private Long memberId;

    @NotNull private String url;

    @NotNull private BigDecimal capacityGb;

    private LocalDateTime generatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Image(
            Album album,
            Long memberId,
            String url,
            LocalDateTime generatedAt,
            BigDecimal capacityGb) {
        this.album = album;
        this.memberId = memberId;
        this.url = url;
        this.generatedAt = generatedAt;
        this.capacityGb = capacityGb;
    }

    public static Image createImage(
            Album album,
            Long memberId,
            String url,
            LocalDateTime generatedAt,
            BigDecimal capacityGb) {
        return Image.builder()
                .album(album)
                .memberId(memberId)
                .url(url)
                .generatedAt(generatedAt)
                .capacityGb(capacityGb)
                .build();
    }
}
