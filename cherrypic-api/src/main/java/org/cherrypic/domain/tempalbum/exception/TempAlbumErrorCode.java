package org.cherrypic.domain.tempalbum.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum TempAlbumErrorCode implements BaseErrorCode {
    CREATE_OVER_LIMIT(400, "임시 앨범은 5개 이상으로 만들 수 없습니다."),
    TEMP_ALBUM_CAPACITY_EXCEEDED(400, "임시 앨범의 용량을 초과했습니다."),
    IMAGES_NOT_IN_TEMP_ALBUM(400, "임시 앨범에 속해 있지 않은 이미지가 포함되어 있습니다."),

    NOT_TEMP_ALBUM_OWNER(403, "임시 앨범 생성자가 아닌 경우 권한이 없습니다."),

    TEMP_ALBUM_NOT_FOUND(404, "임시 앨범이 존재하지 않습니다.");

    private final int status;
    private final String message;
}
