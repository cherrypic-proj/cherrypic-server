package org.cherrypic.domain.image.service;

import org.cherrypic.domain.image.dto.request.MemberProfileImageUploadRequest;
import org.cherrypic.domain.image.dto.response.ImageListResponse;
import org.cherrypic.domain.image.dto.response.PresignedUrlResponse;
import org.cherrypic.global.pagination.SliceResponse;
import org.cherrypic.global.pagination.SortDirection;

public interface ImageService {
    PresignedUrlResponse createMemberProfileImageUploadUrl(MemberProfileImageUploadRequest request);

    SliceResponse<ImageListResponse> getImages(
            Long albumId, Long eventId, Long lastImageId, int size, SortDirection direction);
}
