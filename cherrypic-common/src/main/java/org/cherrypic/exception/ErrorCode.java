package org.cherrypic.exception;

public interface ErrorCode {

    int getStatus();

    default String getCode() {
        return this.toString();
    }

    String getMessage();
}
