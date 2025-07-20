package org.cherrypic.domain.image.service;

import org.cherrypic.domain.image.dto.request.MemberProfileImageUploadRequest;
import org.cherrypic.domain.image.dto.response.PresignedUrlResponse;

public interface ImageService {
    PresignedUrlResponse createMemberProfileImageUploadUrl(MemberProfileImageUploadRequest request);
}
