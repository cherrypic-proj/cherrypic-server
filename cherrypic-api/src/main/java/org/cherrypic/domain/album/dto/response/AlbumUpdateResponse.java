package org.cherrypic.domain.album.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumPlan;

public record AlbumUpdateResponse(
        @Schema(description = "앨범 ID", example = "1") Long albumId,
        @Schema(description = "앨범 이름", example = "연인 앨범") String title,
        @Schema(description = "앨범 커버 URL", example = "https://example.jpg") String coverUrl,
        @Schema(description = "앨범 플랜", example = "BASIC") AlbumPlan plan) {
    public static AlbumUpdateResponse from(Album album) {
        return new AlbumUpdateResponse(
                album.getId(), album.getTitle(), album.getCoverUrl(), album.getPlan());
    }
}
