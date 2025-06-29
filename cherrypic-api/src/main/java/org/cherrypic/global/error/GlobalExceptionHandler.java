package org.cherrypic.global.error;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.cherrypic.exception.CustomException;
import org.cherrypic.exception.ErrorCode;
import org.cherrypic.exception.GlobalErrorCode;
import org.cherrypic.global.response.GlobalResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<GlobalResponse<ErrorResponse>> handleCustomException(
            CustomException e, HttpServletRequest request) {
        log.info("CustomException: code={}, url={}", e.getErrorCode(), request.getRequestURL());

        final ErrorCode errorCode = e.getErrorCode();
        final ErrorResponse errorResponse =
                ErrorResponse.of(errorCode.getCode(), errorCode.getMessage());
        final GlobalResponse<ErrorResponse> response =
                GlobalResponse.fail(errorCode.getStatus(), errorResponse);

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /**
     * javax.validation.Valid or @Validated 으로 binding error 발생시 발생한다. HttpMessageConverter 에서 등록한
     * HttpMessageConverter binding 못할경우 발생 주로 @RequestBody, @RequestPart 어노테이션에서 발생
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<GlobalResponse<ErrorResponse>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        final String errorMessage =
                e.getBindingResult().getAllErrors().getFirst().getDefaultMessage();
        final ErrorResponse errorResponse =
                ErrorResponse.of(e.getClass().getSimpleName(), errorMessage);
        final GlobalResponse<ErrorResponse> response =
                GlobalResponse.fail(HttpStatus.BAD_REQUEST.value(), errorResponse);

        return ResponseEntity.badRequest().body(response);
    }

    /** 지원하지 않은 HTTP method 호출 할 경우 발생 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<GlobalResponse<ErrorResponse>>
            handleHttpRequestMethodNotSupportedException() {
        final ErrorCode errorCode = GlobalErrorCode.METHOD_NOT_ALLOWED;
        final ErrorResponse errorResponse =
                ErrorResponse.of(errorCode.getCode(), errorCode.getMessage());
        final GlobalResponse<ErrorResponse> response =
                GlobalResponse.fail(errorCode.getStatus(), errorResponse);

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /** PathVariable, RequestParam, RequestHeader, RequestBody 에서 타입이 일치하지 않을 경우 발생 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<GlobalResponse<ErrorResponse>>
            handleMethodArgumentTypeMismatchException() {
        final ErrorCode errorCode = GlobalErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH;
        final ErrorResponse errorResponse =
                ErrorResponse.of(errorCode.getCode(), errorCode.getMessage());
        final GlobalResponse<ErrorResponse> response =
                GlobalResponse.fail(errorCode.getStatus(), errorResponse);

        return ResponseEntity.badRequest().body(response);
    }

    /** 500번대 에러 처리 */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<GlobalResponse<ErrorResponse>> handleException() {
        final ErrorCode errorCode = GlobalErrorCode.INTERNAL_SERVER_ERROR;
        final ErrorResponse errorResponse =
                ErrorResponse.of(errorCode.getCode(), errorCode.getMessage());
        final GlobalResponse<ErrorResponse> response =
                GlobalResponse.fail(errorCode.getStatus(), errorResponse);

        return ResponseEntity.internalServerError().body(response);
    }
}
