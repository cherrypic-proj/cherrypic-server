package org.cherrypic.domain.image.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.image.dto.request.AlbumFileUploadRequest;
import org.cherrypic.domain.image.dto.request.AlbumImageDeleteRequest;
import org.cherrypic.domain.image.dto.request.ImageUploadRequest;
import org.cherrypic.domain.image.dto.response.AlbumImageListResponse;
import org.cherrypic.domain.image.dto.response.EventImageListResponse;
import org.cherrypic.domain.image.dto.response.PresignedUrlResponse;
import org.cherrypic.domain.image.dto.response.UploadFileListResponse;
import org.cherrypic.domain.image.service.ImageService;
import org.cherrypic.global.annotation.PageSize;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "2. 이미지 API", description = "이미지 관련 API입니다.")
@Validated
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/members/profile-upload-url")
    @Operation(
            summary = "회원 프로필 이미지 Presigned URL 생성",
            description = "회원 프로필 이미지 업로드를 위한 Presigned URL을 생성합니다.")
    public PresignedUrlResponse memberProfileImageUploadUrlCreate(
            @Valid @RequestBody ImageUploadRequest request) {
        return imageService.createMemberProfileImageUploadUrl(request);
    }

    @PostMapping("/albums/cover-upload-url")
    @Operation(
            summary = "앨범 커버 이미지 Presigned URL 생성",
            description = "앨범 커버 이미지 업로드를 위한 Presigned URL을 생성합니다.")
    public PresignedUrlResponse albumCoverImageUploadUrlCreate(
            @Valid @RequestBody ImageUploadRequest request) {
        return imageService.createAlbumCoverImageUploadUrl(request);
    }

    @PostMapping("/events/cover-upload-url")
    @Operation(
            summary = "이벤트 커버 이미지 Presigned URL 생성",
            description = "이벤트 커버 이미지 업로드를 위한 Presigned URL을 생성합니다.")
    public PresignedUrlResponse eventCoverImageUploadUrlCreate(
            @Valid @RequestBody ImageUploadRequest request) {
        return imageService.createEventCoverImageUploadUrl(request);
    }

    @PostMapping("/albums/{albumId}/images")
    @Operation(
            summary = "앨범 이미지 업로드 Presigned URL들 생성",
            description = "앨범 이미지 업로드를 위한 Presigned URL들을 생성합니다.")
    public UploadFileListResponse albumFileUploadUrlsCreate(
            @PathVariable Long albumId, @Valid @RequestBody AlbumFileUploadRequest request) {
        return imageService.createAlbumFileUploadUrls(albumId, request);
    }

    @GetMapping("/albums/{albumId}/images")
    @Operation(summary = "앨범 이미지 목록 조회", description = "앨범의 이미지 목록을 조회합니다.")
    public SliceResponse<AlbumImageListResponse> albumImagesGet(
            @PathVariable Long albumId,
            @Parameter(description = "이전 페이지의 마지막 이미지 ID (첫 요청 시 생략)")
                    @RequestParam(required = false)
                    Long lastImageId,
            @Parameter(description = "페이지당 조회할 이미지의 수") @RequestParam @PageSize Integer size,
            @Parameter(description = "정렬 방향 (ASC: 오래된순, DESC: 최신순)")
                    @RequestParam(defaultValue = "DESC")
                    SortDirection direction) {
        return imageService.getAlbumImages(albumId, lastImageId, size, direction);
    }

    @GetMapping("/events/{eventId}/images")
    @Operation(summary = "이벤트 이미지 목록 조회", description = "이벤트 이미지 목록을 조회합니다.")
    public SliceResponse<EventImageListResponse> eventImagesGet(
            @PathVariable Long eventId,
            @Parameter(description = "이전 페이지의 마지막 이벤트 이미지 ID (첫 요청 시 생략)")
                    @RequestParam(required = false)
                    Long lastEventImageId,
            @Parameter(description = "페이지당 조회할 이미지의 수") @RequestParam @PageSize Integer size,
            @Parameter(description = "정렬 방향 (ASC: 오래된순, DESC: 최신순)")
                    @RequestParam(defaultValue = "DESC")
                    SortDirection direction) {
        return imageService.getEventImages(eventId, lastEventImageId, size, direction);
    }

    @DeleteMapping("albums/{albumId}/images")
    @Operation(summary = "앨범 이미지 삭제", description = "앨범의 이미지를 삭제합니다.")
    public ResponseEntity<Void> albumImageDelete(
            @PathVariable Long albumId, @Valid @RequestBody AlbumImageDeleteRequest request) {
        imageService.deleteAlbumImage(albumId, request);
        return ResponseEntity.noContent().build();
    }
}
