package org.cherrypic.domain.image.service;

import org.cherrypic.domain.image.dto.request.*;
import org.cherrypic.domain.image.dto.response.*;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;
import org.cherrypic.global.pagination.SortParameter;

public interface ImageService {
    ImageUploadUrlResponse createMemberProfileImageUploadUrl(ImageUploadUrlRequest request);

    ImageUploadUrlResponse createAlbumCoverImageUploadUrl(ImageUploadUrlRequest request);

    ImageUploadUrlResponse createEventCoverImageUploadUrl(ImageUploadUrlRequest request);

    AlbumImagesUploadUrlResponse createAlbumImageUploadUrls(
            Long albumId, AlbumImagesUploadUrlRequest request);

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

    TempAlbumImagesUploadUrlResponse createTempAlbumImageUploadUrls(
            Long tempAlbumId, TempAlbumImagesUploadUrlRequest request);

    void deleteTempAlbumImage(Long tempAlbumId, TempAlbumImageDeleteRequest request);

    void completeNonAlbumImageUpload(ImageUploadCompleteRequest request);

    AlbumImagesUploadCompleteResponse completeAlbumImagesUpload(
            Long albumId, AlbumImagesUploadCompleteRequest request);

    TempAlbumImagesUploadCompleteResponse completeTempAlbumImagesUpload(
            Long tempAlbumId, TempAlbumImagesUploadCompleteRequest request);
}
