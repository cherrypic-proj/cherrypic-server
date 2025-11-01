package org.cherrypic.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record TempAlbumImagesUploadCompleteRequest(
        @NotEmpty(message = "업로드 완료 하고자 하는 임시 앨범 이미지들의 정보들은 비워둘 수 없습니다.")
                @Valid
                @Schema(description = "업로드 완료 요청 리스트")
                List<Payload> payloads) {
    @Schema(name = "TempAlbumImagesUploadCompletePayload")
    public record Payload(
            @NotNull(message = "파일의 용량은 비워둘 수 없습니다.")
                    @Schema(description = "업로드 하는 파일의 용량(MB), 소수점 2자리 까지", example = "0.04")
                    BigDecimal capacityMb,
            @NotBlank(message = "업로드 완료 하고자 하는 tempAlbumImageUrl은 비워둘 수 없습니다.")
                    @Valid
                    @Schema(description = "업로드 완료 요청 임시 앨범 이미지 Url")
                    String tempAlbumImageUrl) {}
}
