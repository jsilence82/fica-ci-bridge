package com.ficabridge.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvoiceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleInvoiceNotFound(InvoiceNotFoundException ex) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @ExceptionHandler(IDocProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleIDocProcessing(IDocProcessingException ex) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
