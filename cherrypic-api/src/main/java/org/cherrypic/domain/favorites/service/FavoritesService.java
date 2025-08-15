package org.cherrypic.domain.favorites.service;

import org.cherrypic.domain.favorites.dto.response.FavoritesMarkToggleResponse;

public interface FavoritesService {
    FavoritesMarkToggleResponse toggleMark(Long albumId);
}
