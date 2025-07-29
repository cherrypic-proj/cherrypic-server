package org.cherrypic.domain.album.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum AlbumErrorCode implements BaseErrorCode {
    ALBUM_NOT_FOUND(404, "앨범이 존재하지 않습니다."),
    NOT_ALBUM_HOST(403, "HOST가 아닌 경우 앨범 초대 링크를 생성할 권한이 없습니다."),
    NOT_ALBUM_PARTICIPANT(403, "앨범에 속하지 않은 사용자입니다."),
    LIMITED_AUTHORITY(403, "앨범에 대한 생성/수정 권한이 없습니다."),

    PAYMENT_REQUIRED_FOR_PAID_PLAN(400, "유료 플랜은 결제 ID가 필요합니다."),
    PAYMENT_NOT_REQUIRED_FOR_BASIC_PLAN(400, "BASIC 플랜에서는 결제 ID가 필요하지 않습니다."),
    ;

    private final int status;
    private final String message;
}
