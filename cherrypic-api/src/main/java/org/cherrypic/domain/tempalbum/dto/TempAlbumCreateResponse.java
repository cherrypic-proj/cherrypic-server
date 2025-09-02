package org.cherrypic.domain.tempalbum.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import org.cherrypic.tempalbum.entity.TempAlbum;
import org.cherrypic.tempalbum.enums.TempAlbumType;

public record TempAlbumCreateResponse(
        @Schema(description = "임시 앨범 ID", example = "1") Long tempAlbumId,
        @Schema(description = "회원 ID", example = "1") Long memberId,
        @Schema(description = "임시 앨범 이름", example = "회사 공유용") String title,
        @Schema(description = "임시 앨범 앨범 유형", example = "DEFAULT") TempAlbumType type) {
    public static TempAlbumCreateResponse from(TempAlbum tempAlbum) {
        return new TempAlbumCreateResponse(
                tempAlbum.getId(),
                tempAlbum.getMember().getId(),
                tempAlbum.getTitle(),
                tempAlbum.getType());
    }
}
