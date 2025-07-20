package org.cherrypic.domain.image.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.image.dto.request.MemberProfileImageUploadRequest;
import org.cherrypic.domain.image.dto.response.PresignedUrlResponse;
import org.cherrypic.domain.image.service.ImageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "2. 이미지 API", description = "이미지 관련 API입니다.")
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/members/me/upload-url")
    @Operation(
            summary = "회원 프로필 이미지 Presigned URL 생성",
            description = "회원 프로필 이미지 업로드를 위한 Presigned URL을 생성합니다.")
    public PresignedUrlResponse memberProfileImageUploadUrlCreate(
            @Valid @RequestBody MemberProfileImageUploadRequest request) {
        return imageService.createMemberProfileImageUploadUrl(request);
    }
}
