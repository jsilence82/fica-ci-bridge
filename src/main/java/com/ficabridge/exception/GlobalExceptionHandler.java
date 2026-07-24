package com.ficabridge.exception;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;

/**
 * Central error handling. Extends {@link ResponseEntityExceptionHandler} so Spring MVC's built-in
 * exceptions (405 method-not-supported, 415 unsupported-media-type, 400 malformed-body, 404
 * no-resource, …) are mapped to their correct HTTP status instead of being swallowed into 500 by
 * the catch-all {@link #handleGeneric}. The base class does the status mapping; {@link
 * #handleExceptionInternal} re-renders every one of those into this project's {@link ErrorResponse}
 * body so the whole API returns a single, consistent error shape.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ── domain / third-party exceptions ──────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RequestNotPermitted ex) {
        return error(HttpStatus.TOO_MANY_REQUESTS,
                "SAP OData rate limit exceeded — too many concurrent calls to the SAP backend. Please retry shortly.");
    }

    @ExceptionHandler(ODataClientException.class)
    public ResponseEntity<ErrorResponse> handleODataClient(ODataClientException ex) {
        return error(HttpStatus.BAD_GATEWAY, "SAP OData upstream error: " + ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        // Anything not mapped above or by the ResponseEntityExceptionHandler base is a genuine
        // server fault: log the real cause, return a generic non-leaking message.
        log.error("Unhandled exception processing request", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    // ── standard Spring MVC exceptions → our ErrorResponse body ───────────────

    /**
     * Every exception the {@link ResponseEntityExceptionHandler} base handles funnels through here.
     * The base has already resolved the correct status; we swap its default {@link ProblemDetail}
     * body for our {@link ErrorResponse}.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        String message;
        if (status.is5xxServerError()) {
            // never leak internals on a server fault
            log.error("Unhandled framework exception processing request", ex);
            message = "An unexpected error occurred";
        } else if (ex instanceof NoResourceFoundException nrfe) {
            // friendlier than the base's "No static resource ..." for the common unmapped-path case
            message = "No endpoint for path: /" + nrfe.getResourcePath();
        } else if (body instanceof ProblemDetail pd && pd.getDetail() != null) {
            // Spring's curated, client-facing description, e.g. "Method 'POST' is not supported."
            message = pd.getDetail();
        } else {
            message = status.getReasonPhrase();
        }

        ErrorResponse errorBody = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message);
        return new ResponseEntity<>(errorBody, headers, statusCode);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
                new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message));
    }
}
