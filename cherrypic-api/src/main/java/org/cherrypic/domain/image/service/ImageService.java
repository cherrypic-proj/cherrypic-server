package org.cherrypic.domain.image.service;

import org.cherrypic.domain.image.dto.request.*;
import org.cherrypic.domain.image.dto.response.*;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.global.pagination.SortParameter;

public interface ImageService {
    PresignedUrlResponse createMemberProfileImageUploadUrl(ImageUploadRequest request);

    PresignedUrlResponse createAlbumCoverImageUploadUrl(ImageUploadRequest request);

    PresignedUrlResponse createEventCoverImageUploadUrl(ImageUploadRequest request);

    AlbumImageUploadResponse createAlbumImageUploadUrls(
            Long albumId, AlbumImageUploadRequest request);

    SliceResponse<AlbumImageListResponse> getAlbumImages(
            Long albumId,
            Long lastImageId,
            int size,
            SortParameter parameter,
            SortDirection direction);

    SliceResponse<EventImageListResponse> getEventImages(
            Long eventId,
            Long lastImageId,
            int size,
            SortParameter parameter,
            SortDirection direction);

    void deleteAlbumImage(Long albumId, AlbumImageDeleteRequest request);

    TempAlbumImageUploadListResponse createTempAlbumImageUploadUrls(
            Long tempAlbumId, TempAlbumImageUploadRequest request);

    void deleteTempAlbumImage(Long tempAlbumId, TempAlbumImageDeleteRequest request);

    void confirmNonAlbumImageUpload(ImageConfirmRequest request);

    AlbumImagesConfirmResponse confirmAlbumImagesUpload(
            Long albumId, AlbumImagesConfirmRequest request);
}
