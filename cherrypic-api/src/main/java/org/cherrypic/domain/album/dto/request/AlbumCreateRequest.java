package org.cherrypic.domain.album.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.cherrypic.album.enums.AlbumPlan;
import org.cherrypic.global.annotation.Enum;

public record AlbumCreateRequest(
        @NotBlank(message = "앨범 이름은 비워둘 수 없습니다.")
                @Schema(description = "앨범 이름", example = "연인 앨범")
                @Size(max = 20, message = "앨범 이름은 최대 20자까지 가능합니다.")
                String title,
        @Schema(description = "앨범 커버 URL", example = "https://example.jpg") String coverUrl,
        @Enum(message = "앨범 플랜은 비워둘 수 없으며, BASIC, PRO, PREMIUM만 지원됩니다.")
                @Schema(description = "앨범 플랜 (BASIC: 무료, PRO/PREMIUM: 유료)", example = "BASIC")
                AlbumPlan plan,
        @Schema(description = "유료 플랜(PRO, PREMIUM)인 경우 필수. 결제 검증 후 받은 결제 ID", example = "1")
                Long paymentId) {}
