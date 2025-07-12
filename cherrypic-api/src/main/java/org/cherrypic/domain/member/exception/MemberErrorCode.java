package org.cherrypic.domain.member.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {
    MEMBER_NOT_FOUND(404, "회원을 찾을 수 없습니다."),
    ;

    private final int status;
    private final String message;
}
