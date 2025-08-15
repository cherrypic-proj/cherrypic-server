package org.cherrypic.domain.favorites.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.favorites.dto.response.FavoritesMarkToggleResponse;
import org.cherrypic.domain.favorites.service.FavoritesService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/albums/{albumId}/favorites")
@RequiredArgsConstructor
@Tag(name = "7. 즐겨찾기 API", description = "즐겨찾기 관련 API입니다.")
public class FavoritesController {

    private final FavoritesService favoritesService;

    @PatchMapping
    @Operation(summary = "앨범 즐겨찾기 토글", description = "앨범의 즐겨찾기 상태를 토글합니다.")
    public FavoritesMarkToggleResponse markToggle(@PathVariable Long albumId) {
        return favoritesService.toggleMark(albumId);
    }
}
