package org.cherrypic.domain.album.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cherrypic.album.enums.AlbumType;
import org.cherrypic.domain.album.dto.request.AlbumCreateRequest;
import org.cherrypic.domain.album.dto.request.AlbumUpdateRequest;
import org.cherrypic.domain.album.dto.response.*;
import org.cherrypic.domain.album.service.AlbumService;
import org.cherrypic.global.annotation.PageSize;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/albums")
@RequiredArgsConstructor
@Tag(name = "3. 앨범 API", description = "앨범 관련 API입니다.")
@Validated
public class AlbumController {

    private final AlbumService albumService;

    @PostMapping
    @Operation(summary = "앨범 생성", description = "새로운 앨범을 생성합니다.")
    public ResponseEntity<AlbumCreateResponse> albumCreate(
            @Valid @RequestBody AlbumCreateRequest request) {
        AlbumCreateResponse response = albumService.createAlbum(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{albumId}")
    @Operation(summary = "앨범 수정", description = "앨범의 이름과 커버를 수정합니다.")
    public AlbumUpdateResponse albumUpdate(
            @PathVariable Long albumId, @Valid @RequestBody AlbumUpdateRequest request) {
        return albumService.updateAlbum(albumId, request);
    }

    @PatchMapping("/{albumId}/permission")
    @Operation(summary = "앨범 권한 부여 토글 상태 변경", description = "앨범의 권한 부여 토글 상태를 변경합니다.")
    public PermissionToggleResponse permissionToggle(@PathVariable Long albumId) {
        return albumService.togglePermission(albumId);
    }

    @PostMapping("/{albumId}/invitation-link")
    @Operation(summary = "앨범 초대 링크 생성", description = "앨범 초대링크를 생성합니다.")
    public ResponseEntity<InvitationLinkCreateResponse> invitationLinkCreate(
            @PathVariable Long albumId) {
        InvitationLinkCreateResponse response = albumService.createInvitationLink(albumId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{albumId}/join")
    @Operation(summary = "앨범 입장", description = "앨범에 입장합니다.")
    public ResponseEntity<AlbumJoinResponse> albumJoin(
            @PathVariable Long albumId,
            @Parameter(description = "앨범의 초대 코드") @RequestParam String code) {
        AlbumJoinResponse response = albumService.joinAlbum(albumId, code);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{albumId}")
    @Operation(summary = "개별 앨범 조회", description = "개별 앨범을 조회합니다.")
    public AlbumInfoResponse albumGet(@PathVariable Long albumId) {
        return albumService.getAlbum(albumId);
    }

    @GetMapping
    @Operation(
            summary = "앨범 목록 조회",
            description =
                    "회원이 참여 중인 앨범 목록을 커서 기반 페이징 방식으로 조회합니다. 앨범 유형을 지정하면 해당 유형에 해당하는 앨범만 필터링하여 조회합니다.")
    public SliceResponse<AlbumListResponse> albumsGet(
            @Parameter(description = "앨범 유형 (BASIC, PRO, PREMIUM). 생략 시 전체 조회")
                    @RequestParam(required = false)
                    AlbumType type,
            @Parameter(description = "검색 키워드 (앨범 제목에 포함된 단어). 생략 시 전체 조회")
                    @RequestParam(required = false)
                    String keyword,
            @Parameter(description = "이전 페이지의 마지막 앨범 ID (첫 요청 시 생략)")
                    @RequestParam(required = false)
                    Long lastAlbumId,
            @Parameter(description = "페이지당 조회할 앨범 수") @RequestParam @PageSize Integer size,
            @Parameter(description = "정렬 방향 (ASC: 오래된순, DESC: 최신순)")
                    @RequestParam(defaultValue = "DESC")
                    SortDirection direction) {
        return albumService.getParticipatingAlbumsByCondition(
                type, keyword, lastAlbumId, size, direction);
    }

    @DeleteMapping("/{albumId}")
    @Operation(summary = "앨범 삭제", description = "앨범을 삭제합니다.")
    public ResponseEntity<Void> albumDelete(@PathVariable Long albumId) {
        albumService.deleteAlbum(albumId);
        return ResponseEntity.noContent().build();
    }
}
