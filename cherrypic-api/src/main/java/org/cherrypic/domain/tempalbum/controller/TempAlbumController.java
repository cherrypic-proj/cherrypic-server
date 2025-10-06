package org.cherrypic.domain.tempalbum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.tempalbum.dto.request.TempAlbumCreateRequest;
import org.cherrypic.domain.tempalbum.dto.request.TempAlbumUpdateRequest;
import org.cherrypic.domain.tempalbum.dto.response.TempAlbumCreateResponse;
import org.cherrypic.domain.tempalbum.dto.response.TempAlbumListResponse;
import org.cherrypic.domain.tempalbum.service.TempAlbumService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/temp-albums")
@Tag(name = "9. 임시 앨범 API", description = "임시 앨범 관련 API입니다.")
public class TempAlbumController {

    private final TempAlbumService tempAlbumService;

    @PostMapping
    @Operation(summary = "임시 앨범 생성", description = "새로운 임시 앨범을 생성합니다.")
    public ResponseEntity<TempAlbumCreateResponse> tempAlbumCreate(
            @Valid @RequestBody TempAlbumCreateRequest request) {
        TempAlbumCreateResponse response = tempAlbumService.createTempAlbum(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "임시 앨범 목록 조회", description = "회원이 가지고 있는 임시 앨범을 조회합니다.")
    public TempAlbumListResponse tempAlbumsGet() {
        return tempAlbumService.getTempAlbums();
    }

    @PatchMapping("/{tempAlbumId}")
    @Operation(summary = "임시 앨범 수정", description = "임시 앨범을 수정합니다.")
    public ResponseEntity<Void> tempAlbumUpdate(
            @PathVariable Long tempAlbumId, @Valid @RequestBody TempAlbumUpdateRequest request) {
        tempAlbumService.updateTempAlbum(tempAlbumId, request);
        return ResponseEntity.noContent().build();
    }
}
