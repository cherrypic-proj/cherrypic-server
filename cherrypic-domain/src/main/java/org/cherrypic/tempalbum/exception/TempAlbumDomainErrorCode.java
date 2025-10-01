package org.cherrypic.tempalbum.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum TempAlbumDomainErrorCode implements BaseErrorCode {
    TEMP_ALBUM_CAPACITY_INCREASE_OVER_LIMIT(400, "임시 앨범의 용량을 초과했습니다."),
    TEMP_ALBUM_CAPACITY_DECREASE_UNDER_ZERO(400, "임시 앨범 용량은 0MB 미만으로 줄일 수 없습니다.");

    private final int status;
    private final String message;
}
