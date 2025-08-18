package org.cherrypic.domain.image.service;

import org.cherrypic.domain.image.dto.request.AlbumImageUploadRequest;
import org.cherrypic.domain.image.dto.request.MemberProfileImageUploadRequest;
import org.cherrypic.domain.image.dto.response.AlbumImageListResponse;
import org.cherrypic.domain.image.dto.response.EventImageListResponse;
import org.cherrypic.domain.image.dto.response.PresignedUrlResponse;
import org.cherrypic.domain.image.dto.response.PresignedUrlsResponse;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;

public interface ImageService {
    PresignedUrlResponse createMemberProfileImageUploadUrl(MemberProfileImageUploadRequest request);

    PresignedUrlsResponse createAlbumImageUploadUrls(AlbumImageUploadRequest request, Long albumId);

    SliceResponse<AlbumImageListResponse> getAlbumImages(
            Long albumId, Long lastImageId, int size, SortDirection direction);

    SliceResponse<EventImageListResponse> getEventImages(
            Long eventId, Long lastImageId, int size, SortDirection direction);
}
