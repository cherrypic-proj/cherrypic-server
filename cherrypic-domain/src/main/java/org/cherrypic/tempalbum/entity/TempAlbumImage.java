package org.cherrypic.tempalbum.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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

    @Builder(access = AccessLevel.PRIVATE)
    private TempAlbumImage(TempAlbum tempAlbum, String url) {
        this.tempAlbum = tempAlbum;
        this.url = url;
    }

    public static TempAlbumImage createTempAlbumImage(TempAlbum tempAlbum, String url) {
        return TempAlbumImage.builder().tempAlbum(tempAlbum).url(url).build();
    }
}
