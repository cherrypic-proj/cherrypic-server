package org.cherrypic.domain.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.album.entity.Album;
import org.cherrypic.event.entity.Event;

public record EventCreateResponse(
        @Schema(description = "엘범 ID", example = "1L") Long albumId,
        @Schema(description = "이벤트 ID", example = "1L") Long eventId,
        @Schema(description = "이벤트 제목", example = "일본 여행") String eventTitle) {
    public static EventCreateResponse from(Event event, Album album) {
        return new EventCreateResponse(album.getId(), event.getId(), event.getName());
    }
}
