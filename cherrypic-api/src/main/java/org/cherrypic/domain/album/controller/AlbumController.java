package org.cherrypic.domain.album.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.dto.response.AlbumCreateResponse;
import org.cherrypic.domain.album.dto.response.AlbumListResponse;
import org.cherrypic.domain.album.dto.response.InvitationLinkCreateResponse;
import org.cherrypic.domain.album.service.AlbumService;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/albums")
@RequiredArgsConstructor
@Tag(name = "3. 앨범 API", description = "앨범 관련 API입니다.")
public class AlbumController {

    private final AlbumService albumService;

    @PostMapping
    @Operation(summary = "앨범 생성", description = "새로운 앨범을 생성합니다.")
    public ResponseEntity<AlbumCreateResponse> albumCreate(
            @Valid @RequestBody AlbumCreateRequest request) {
        AlbumCreateResponse response = albumService.createAlbum(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{albumId}/invitation-link")
    @Operation(summary = "앨범 초대 링크 생성", description = "앨범 초대링크를 생성합니다.")
    public ResponseEntity<InvitationLinkCreateResponse> invitationLinkCreate(
            @PathVariable Long albumId) {
        InvitationLinkCreateResponse response = albumService.createInvitationLink(albumId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "앨범 목록 조회", description = "회원이 참여 중인 앨범 목록을 커서 기반 페이징 방식으로 조회합니다.")
    public SliceResponse<AlbumListResponse> albums(
            @Parameter(description = "이전 페이지의 마지막 앨범 ID (첫 요청 시 생략)")
                    @RequestParam(required = false)
                    Long lastAlbumId,
            @Parameter(description = "페이지당 조회할 앨범 수") @RequestParam int size,
            @Parameter(description = "정렬 방향 (ASC: 오래된순, DESC: 최신순)")
                    @RequestParam(defaultValue = "DESC")
                    SortDirection direction) {
        return albumService.getParticipatingAlbums(lastAlbumId, size, direction);
    }
}
