package org.cherrypic.image.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.common.model.BaseTimeEntity;
import org.cherrypic.event.entity.EventImage;

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

    private LocalDateTime generatedAt;

    @OneToMany(mappedBy = "image", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventImage> eventImages = new ArrayList<>();

<<<<<<< HEAD
    @Builder
    private Image(Album album, Long memberId, String url, LocalDateTime generatedAt) {
        this.album = album;
        this.memberId = memberId;
        this.url = url;
        this.generatedAt = generatedAt;
    }

    public static Image createImage(
            Album album, Long memberId, String url, LocalDateTime generatedAt) {
=======
    @Builder(access = AccessLevel.PRIVATE)
    private Image(Album album, Long memberId, String url, LocalDateTime imageFileCreatedAt) {
        this.album = album;
        this.memberId = memberId;
        this.url = url;
        this.imageFileCreatedAt = imageFileCreatedAt;
    }

    public static Image createImage(
            Album album, Long memberId, String url, LocalDateTime imageFileCreatedAt) {
>>>>>>> 81978e8 (feat: image 엔티티 생성자 구현)
        return Image.builder()
                .album(album)
                .memberId(memberId)
                .url(url)
<<<<<<< HEAD
                .generatedAt(generatedAt)
=======
                .imageFileCreatedAt(imageFileCreatedAt)
>>>>>>> 81978e8 (feat: image 엔티티 생성자 구현)
                .build();
    }
}
