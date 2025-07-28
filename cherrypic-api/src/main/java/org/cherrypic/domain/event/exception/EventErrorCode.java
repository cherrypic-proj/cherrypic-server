package org.cherrypic.domain.event.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum EventErrorCode implements BaseErrorCode {
    NOT_ALBUM_PARTICIPANT(403, "참여하지 않은 앨범에는 이벤트를 생성할 권한이 없습니다"),
    EVENT_NOT_FOUND(404, "존재하지 않는 이밴트입니다."),
    LIMITED_AUTHORITY(403, "LIMITED 유저는 앨범에 대한 생성/수정 권한이 없습니다.");

    private final int status;
    private final String message;
}
