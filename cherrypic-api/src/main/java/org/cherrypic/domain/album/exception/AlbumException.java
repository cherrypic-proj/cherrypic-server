package org.cherrypic.domain.album.exception;

import org.cherrypic.exception.BaseCustomException;

public class AlbumException extends BaseCustomException {
    public AlbumException(AlbumErrorCode errorCode) {
        super(errorCode);
    }
}
