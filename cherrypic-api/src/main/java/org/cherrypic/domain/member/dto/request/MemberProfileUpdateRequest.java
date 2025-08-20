package org.cherrypic.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberProfileUpdateRequest(
        @NotBlank(message = "닉네임은 비워둘 수 없습니다.")
                @Pattern(regexp = "^[0-9a-zA-Z가-힣 ]+$", message = "닉네임에는 특수문자를 포함할 수 없습니다.")
                @Size(max = 15, message = "닉네임은 최대 15자까지 입력 가능합니다.")
                @Schema(description = "닉네임", example = "최현태")
                String nickname,
        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
                String profileImageUrl) {}
