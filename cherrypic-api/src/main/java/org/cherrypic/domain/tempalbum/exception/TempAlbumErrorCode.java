package org.cherrypic.domain.tempalbum.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum TempAlbumErrorCode implements BaseErrorCode {
    CREATE_OVER_LIMIT(400, "임시 앨범은 5개 이상으로 만들 수 없습니다.");

    private final int status;
    private final String message;
}
