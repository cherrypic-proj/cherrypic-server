package org.cherrypic.domain.subscription.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum SubscriptionErrorCode implements BaseErrorCode {
    SUBSCRIPTION_NOT_FOUND(404, "구독 정보가 존재하지 않습니다."),
    SUBSCRIPTION_NOT_SUPPORTED_FOR_BASIC_PLAN(400, "BASIC 플랜은 구독 기능을 지원하지 않습니다."),
    SUBSCRIPTION_ALREADY_CANCELED(409, "이미 해지된 구독입니다."),
    SUBSCRIPTION_ALREADY_ENDED(400, "이미 종료된 구독입니다. 해지 또는 갱신할 수 없습니다."),
    ;

    private final int status;
    private final String message;
}
