package org.cherrypic.domain.album.exception;

import org.cherrypic.exception.BaseCustomException;
import org.cherrypic.exception.BaseErrorCode;

public class AlbumException extends BaseCustomException {
    public AlbumException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
