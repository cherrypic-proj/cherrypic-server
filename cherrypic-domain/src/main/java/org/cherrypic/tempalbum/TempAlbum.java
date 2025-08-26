package org.cherrypic.tempalbum;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cherrypic.album.entity.Album;
import org.cherrypic.common.model.BaseTimeEntity;
import org.cherrypic.member.entity.Member;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private Album album;

    @NotNull private LocalDate expiredAt;

    @OneToMany(mappedBy = "tempAlbum", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TempAlbumImage> tempAlbumImages = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private TempAlbum(Album album, Member member, LocalDate expiredAt) {
        this.album = album;
        this.member = member;
        this.expiredAt = expiredAt;
    }

    public static TempAlbum createTempAlbum(Album album, Member member, LocalDate expiredAt) {
        return TempAlbum.builder().album(album).member(member).expiredAt(expiredAt).build();
    }

    public void addTempAlbumImages(List<TempAlbumImage> images) {
        this.tempAlbumImages.addAll(images);
    }
}
