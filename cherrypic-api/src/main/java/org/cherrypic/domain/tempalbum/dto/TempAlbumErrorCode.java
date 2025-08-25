package org.cherrypic.domain.tempalbum.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum TempAlbumErrorCode implements BaseErrorCode {
    TEMP_ALBUM_UPLOAD_FAIL(503, "임시 앨범 업로드에 실패했습니다.");

    private final int status;
    private final String message;
}
