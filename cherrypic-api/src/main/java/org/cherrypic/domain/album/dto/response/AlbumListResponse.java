package org.cherrypic.domain.album.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.album.enums.AlbumType;

public record AlbumListResponse(
        @Schema(description = "앨범 ID", example = "1") Long albumId,
        @Schema(description = "앨범 이름", example = "연인 앨범") String title,
        @Schema(description = "앨범 커버 URL", example = "https://example.jpg") String coverUrl,
        @Schema(description = "앨범 유형", example = "BASIC") AlbumType type,
        @Schema(description = "즐겨찾기 상태", example = "true") Boolean marked) {}
