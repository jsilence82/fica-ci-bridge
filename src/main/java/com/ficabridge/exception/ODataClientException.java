package com.ficabridge.exception;

public class ODataClientException extends RuntimeException {

    private final int httpStatus;

    public ODataClientException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public ODataClientException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = 0;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
