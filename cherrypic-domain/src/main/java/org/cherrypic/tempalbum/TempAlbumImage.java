package org.cherrypic.tempalbum;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.common.model.BaseTimeEntity;
import org.cherrypic.image.entity.Image;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private Image image;

    @Builder(access = AccessLevel.PRIVATE)
    private TempAlbumImage(TempAlbum tempAlbum, Image image) {
        this.tempAlbum = tempAlbum;
        this.image = image;
    }

    public static TempAlbumImage createTempAlbumImage(TempAlbum tempAlbum, Image image) {
        return TempAlbumImage.builder().tempAlbum(tempAlbum).image(image).build();
    }
}
