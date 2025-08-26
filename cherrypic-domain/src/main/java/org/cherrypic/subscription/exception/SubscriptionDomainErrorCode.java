package org.cherrypic.subscription.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum SubscriptionDomainErrorCode implements BaseErrorCode {
    ALREADY_CANCELED(409, "이미 해지된 구독입니다."),
    ALREADY_ACTIVE(409, "이미 구독 중인 상태입니다."),
    ALREADY_ENDED(400, "이미 종료된 구독입니다. 해지 또는 갱신할 수 없습니다."),
    ;

    private final int status;
    private final String message;
}
