package org.cherrypic.domain.participant.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum ParticipantErrorCode implements BaseErrorCode {
    PARTICIPANT_NOT_FOUND(404, "참가자를 찾을 수 없습니다."),

    MISSING_CURSOR_PAIR(400, "lastNickname과 lastParticipantId는 요청에 함께 포함되어야 합니다. 하나만 포함할 수는 없습니다.");

    private final int status;
    private final String message;
}
