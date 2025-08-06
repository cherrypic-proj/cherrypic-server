package org.cherrypic.domain.album.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.album.entity.Album;

public record PermissionToggleResponse(
        @Schema(description = "권한 부여 토글 상태", example = "true") Boolean permissionControl) {
    public static PermissionToggleResponse from(Album album) {
        return new PermissionToggleResponse(album.getPermissionControl());
    }
}
