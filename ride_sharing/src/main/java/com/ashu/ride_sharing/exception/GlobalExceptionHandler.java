package com.ashu.ride_sharing.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import io.jsonwebtoken.security.SignatureException;


import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler{

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorDetails> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex , WebRequest request){
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), ex.getMessage(),request.getDescription(false));
        log.warn("Conflict: {}", ex.getMessage());
        return new ResponseEntity<>(errorDetails,HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorDetails> handleInvalidTokenException(InvalidTokenException ex, WebRequest request){
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), ex.getMessage(), request.getDescription(false));
        log.warn("Bad Request/Unauthorized: {}", ex.getMessage());
        // Decide status based on typical use (e.g., verification -> Bad Request, refresh -> Unauthorized)
        // Here using BAD_REQUEST as a default for invalid structure/expiry during verification
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST); // 400
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorDetails> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), "Invalid credentials", request.getDescription(false));
        log.warn("Unauthorized: Invalid credentials provided.");
        return new ResponseEntity<>(errorDetails, HttpStatus.UNAUTHORIZED); // 401
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDetails> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), "Access denied", request.getDescription(false));
        log.warn("Forbidden: {}", ex.getMessage());
        return new ResponseEntity<>(errorDetails, HttpStatus.FORBIDDEN); // 403
    }

    // --- Handle JWT specific exceptions from the filter ---
    // Note: These might be logged in the filter but not reach here if the filter chain stops.
    // If you configure the filter to proceed, these handlers can catch them.
    @ExceptionHandler({ ExpiredJwtException.class, SignatureException.class, MalformedJwtException.class })
    public ResponseEntity<ErrorDetails> handleJwtExceptions(Exception ex, WebRequest request) {
        String message = "Invalid or expired JWT token";
        if (ex instanceof ExpiredJwtException) message = "JWT token has expired";
        else if (ex instanceof SignatureException) message = "JWT signature validation failed";
        else if (ex instanceof MalformedJwtException) message = "JWT token is malformed";

        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), message, request.getDescription(false));
        log.warn("Unauthorized (JWT): {}", message);
        return new ResponseEntity<>(errorDetails, HttpStatus.UNAUTHORIZED); // 401
    }


    // Handle validation errors (@Valid DTOs) - Override default handler
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("errors", errors); // Map of field -> error message
        body.put("path", request.getDescription(false));

        log.warn("Validation failed: {}", errors);
        return new ResponseEntity<>(body, headers, status); // Usually 400 Bad Request
    }


    // Catch-all for other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), "An internal error occurred", request.getDescription(false));
        log.error("Internal Server Error: ", ex); // Log the full stack trace for internal errors
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR); // 500
    }

    // Simple ErrorDetails class
    public record ErrorDetails(LocalDateTime timestamp, String message, String details) {}
}
