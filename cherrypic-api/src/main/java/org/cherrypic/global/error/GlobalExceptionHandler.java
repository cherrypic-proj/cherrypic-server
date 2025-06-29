package org.cherrypic.global.error;

import org.cherrypic.exception.CustomException;
import org.cherrypic.exception.ErrorCode;
import org.cherrypic.global.response.GlobalResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<GlobalResponse<ErrorResponse>> handleCustomException(
            CustomException e) {
        final ErrorCode errorCode = e.getErrorCode();
        final ErrorResponse errorResponse =
                ErrorResponse.of(errorCode.getCode(), errorCode.getMessage());
        final GlobalResponse<ErrorResponse> response =
                GlobalResponse.fail(errorCode.getStatus(), errorResponse);

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }
}
