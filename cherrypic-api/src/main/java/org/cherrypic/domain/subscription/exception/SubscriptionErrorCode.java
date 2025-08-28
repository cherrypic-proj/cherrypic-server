package org.cherrypic.domain.subscription.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cherrypic.exception.BaseErrorCode;

@Getter
@AllArgsConstructor
public enum SubscriptionErrorCode implements BaseErrorCode {
    SUBSCRIPTION_NOT_FOUND(404, "구독 정보가 존재하지 않습니다."),
    SUBSCRIPTION_NOT_SUPPORTED_FOR_BASIC_TYPE(400, "BASIC 유형은 구독 기능을 지원하지 않습니다."),
    ;

    private final int status;
    private final String message;
}
