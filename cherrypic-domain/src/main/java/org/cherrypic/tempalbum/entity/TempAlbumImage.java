package org.cherrypic.tempalbum.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.common.model.BaseTimeEntity;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TempAlbumImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "temp_album_id")
    private TempAlbum tempAlbum;

    @NotNull private String url;

    @NotNull private BigDecimal capacityMb;

    @Builder(access = AccessLevel.PRIVATE)
    private TempAlbumImage(TempAlbum tempAlbum, String url, BigDecimal capacityMb) {
        this.tempAlbum = tempAlbum;
        this.url = url;
        this.capacityMb = capacityMb;
    }

    public static TempAlbumImage createTempAlbumImage(
            TempAlbum tempAlbum, String url, BigDecimal capacityMb) {
        return TempAlbumImage.builder()
                .tempAlbum(tempAlbum)
                .url(url)
                .capacityMb(capacityMb)
                .build();
    }
}
