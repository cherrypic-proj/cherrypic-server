package org.cherrypic.domain.tempalbum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.album.dto.request.TempAlbumRequest;
import org.cherrypic.domain.tempalbum.service.TempAlbumService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "9. 임시 앨범 API", description = "임시 앨범 관련 API입니다.")
public class TempAlbumController {

    private final TempAlbumService tempAlbumService;

    // 로직은 QR 반환을 고려하고 있음 근데 내 임시 앨범들을 볼려면 DB에 넣어놔야 하긴할듯.
    // 발급 제한을 두지는 않을 건지..? 이런거는 생각해봐야함 -> 용량이 부담되게 늘진 않아요 ! -> 참조 형식을 쓰기 때문 대신 원본이 삭제되면  고장날듯
    @PostMapping("/album/{albumId}/temp-album")
    @Operation(summary = "임시 앨범 생성 API", description = "임시 앨범을 생성합니다.")
    public ResponseEntity<Void> tempAlbumCreate(
            @PathVariable Long albumId, @Valid @RequestBody TempAlbumRequest request) {
        tempAlbumService.createTempAlbum(albumId, request);
        return ResponseEntity.noContent().build();
    }
}
