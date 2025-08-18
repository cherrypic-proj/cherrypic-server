package org.cherrypic.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum DomainErrorCode implements BaseErrorCode {
    ALBUM_CAPACITY_INCREASE_OVER_LIMIT(400, "앨범 용량은 9999GB 초과로 늘릴 수 없습니다"),
    ALBUM_CAPACITY_DECREASE_UNDER_ZERO(400, "앨범 용량은 0GB 미만으로 줄일 수 없습니다.");

    private final int status;
    private final String message;
}
