package org.cherrypic.domain.tempalbum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.tempalbum.dto.TempAlbumCreateRequest;
import org.cherrypic.domain.tempalbum.dto.TempAlbumCreateResponse;
import org.cherrypic.domain.tempalbum.service.TempAlbumService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
