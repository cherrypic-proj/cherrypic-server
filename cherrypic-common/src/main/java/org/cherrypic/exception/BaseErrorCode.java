package org.cherrypic.exception;

public interface BaseErrorCode {

    int getStatus();

    default String getCode() {
        return this.toString();
    }

    String getMessage();
}
