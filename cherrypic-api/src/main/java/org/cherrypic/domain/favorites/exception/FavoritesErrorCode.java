package org.cherrypic.domain.favorites.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum FavoritesErrorCode implements BaseErrorCode {
    FAVORITES_NOT_FOUND(404, "참가자에 대한 즐겨찾기 정보가 존재하지 않습니다."),
    ;

    private final int status;
    private final String message;
}
