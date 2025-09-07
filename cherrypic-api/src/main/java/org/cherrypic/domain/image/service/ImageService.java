package org.cherrypic.domain.image.service;

import org.cherrypic.domain.image.dto.request.AlbumImageDeleteRequest;
import org.cherrypic.domain.image.dto.request.AlbumImageUploadRequest;
import org.cherrypic.domain.image.dto.request.ImageUploadRequest;
import org.cherrypic.domain.image.dto.response.AlbumImageListResponse;
import org.cherrypic.domain.image.dto.response.EventImageListResponse;
import org.cherrypic.domain.image.dto.response.ImageUploadListResponse;
import org.cherrypic.domain.image.dto.response.PresignedUrlResponse;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;

public interface ImageService {
    PresignedUrlResponse createMemberProfileImageUploadUrl(ImageUploadRequest request);

    PresignedUrlResponse createAlbumCoverImageUploadUrl(ImageUploadRequest request);

    PresignedUrlResponse createEventCoverImageUploadUrl(ImageUploadRequest request);

    ImageUploadListResponse createAlbumImageUploadUrls(
            Long albumId, AlbumImageUploadRequest request);

    SliceResponse<AlbumImageListResponse> getAlbumImages(
            Long albumId, Long lastImageId, int size, SortDirection direction);

    SliceResponse<EventImageListResponse> getEventImages(
            Long eventId, Long lastImageId, int size, SortDirection direction);

    void deleteAlbumImage(Long albumId, AlbumImageDeleteRequest request);
}
