package org.cherrypic.tempalbum.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.album.entity.Image;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TempAlbumImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "temp_album_id", nullable = false)
    private TempAlbum tempAlbum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;
}
