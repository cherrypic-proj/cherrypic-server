package org.cherrypic.domain.album.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum AlbumErrorCode implements BaseErrorCode {
    ALBUM_NOT_FOUND(404, "엘범이 존재하지 않습니다."),
    ;

    private final int status;
    private final String message;
}
