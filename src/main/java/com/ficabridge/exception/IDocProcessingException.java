package com.ficabridge.exception;

public class IDocProcessingException extends RuntimeException {

    public IDocProcessingException(String message) {
        super(message);
    }

    public IDocProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
