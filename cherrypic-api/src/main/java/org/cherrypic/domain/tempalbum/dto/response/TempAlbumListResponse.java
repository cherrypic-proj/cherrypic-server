package org.cherrypic.domain.tempalbum.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.cherrypic.tempalbum.entity.TempAlbum;

public record TempAlbumListResponse(List<Content> contents) {
    @Schema(name = "TempAlbumListResponseContent")
    public record Content(
            @Schema(description = "임시 앨범 ID", example = "1") Long tempAlbumId,
            @Schema(description = "임시 앨범 이름", example = "회사 공유용") String title) {}

    public static TempAlbumListResponse from(List<TempAlbum> tempAlbums) {
        List<Content> content =
                tempAlbums.stream()
                        .map(album -> new Content(album.getId(), album.getTitle()))
                        .toList();

        return new TempAlbumListResponse(content);
    }
}
