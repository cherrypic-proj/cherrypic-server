package org.cherrypic.domain.favorites.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cherrypic.favorites.entity.Favorites;

public record FavoritesMarkToggleResponse(
        @Schema(description = "즐겨찾기 상태", example = "true") Boolean marked) {
    public static FavoritesMarkToggleResponse from(Favorites favorites) {
        return new FavoritesMarkToggleResponse(favorites.getMarked());
    }
}
