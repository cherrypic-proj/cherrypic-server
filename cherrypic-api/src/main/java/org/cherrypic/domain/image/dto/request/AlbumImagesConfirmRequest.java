package org.cherrypic.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AlbumImagesConfirmRequest(
        @NotEmpty(message = "검증하고자 하는 이미지들의 정보들은 비워둘 수 없습니다.")
                @Valid
                @Schema(description = "검증 요청 리스트")
                List<Payload> payloads) {
    @Schema(name = "AlbumImagesConfirmPayload")
    public record Payload(
            String md5Hashes,
            @Schema(description = "파일이 찍힌 시간, 정보가 없다면 null을 넣어주세요.") LocalDateTime generatedAt,
            @NotNull(message = "파일의 용량은 비워둘 수 없습니다.")
                    @Schema(description = "업로드 하는 파일의 용량(MB), 소수점 2자리 까지", example = "0.04")
                    BigDecimal capacityMb,
            @NotEmpty(message = "검증하고자 하는 imageUrl은 비워둘 수 없습니다.")
                    @Valid
                    @Schema(description = "검증 요청 이미지 Url")
                    String imageUrl) {}
}
