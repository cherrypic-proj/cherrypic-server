package org.cherrypic.domain.album.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.album.entity.Album;
import org.cherrypic.album.enums.AlbumType;

public record AlbumCreateResponse(
        @Schema(description = "앨범 ID", example = "1") Long albumId,
        @Schema(description = "앨범 이름", example = "연인 앨범") String title,
        @Schema(description = "앨범 커버 URL", example = "https://example.jpg") String coverUrl,
        @Schema(description = "앨범 유형", example = "BASIC") AlbumType type,
        @Schema(description = "권한 부여 활성화 여부", example = "false") Boolean permissionControl) {
    public static AlbumCreateResponse from(Album album) {
        return new AlbumCreateResponse(
                album.getId(),
                album.getTitle(),
                album.getCoverUrl(),
                album.getType(),
                album.getPermissionControl());
    }
}
