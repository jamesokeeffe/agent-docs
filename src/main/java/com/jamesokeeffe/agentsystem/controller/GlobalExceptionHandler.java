package com.jamesokeeffe.agentsystem.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the REST API.
 * 
 * This class handles exceptions thrown by controllers and converts them
 * into appropriate HTTP responses with consistent error formatting.
 * 
 * @author James O'Keeffe
 * @version 1.0.0
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation errors from @Valid annotations.
     * 
     * @param ex the validation exception
     * @param request the web request
     * @return validation error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        logger.warn("Validation error: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.error(
            "VALIDATION_ERROR", 
            "Input validation failed", 
            errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles illegal argument exceptions.
     * 
     * @param ex the illegal argument exception
     * @param request the web request
     * @return bad request response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        logger.warn("Illegal argument: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.error("INVALID_ARGUMENT", ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles illegal state exceptions.
     * 
     * @param ex the illegal state exception
     * @param request the web request
     * @return conflict response
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {
        
        logger.warn("Illegal state: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.error("INVALID_STATE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handles runtime exceptions (catch-all).
     * 
     * @param ex the runtime exception
     * @param request the web request
     * @return internal server error response
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        logger.error("Runtime exception: ", ex);
        
        // Don't expose internal error details in production
        String message = "An internal error occurred. Please try again later.";
        
        // In development, you might want to show more details
        // String message = ex.getMessage();
        
        ApiResponse<Void> response = ApiResponse.error("INTERNAL_ERROR", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handles general exceptions.
     * 
     * @param ex the exception
     * @param request the web request
     * @return internal server error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, WebRequest request) {
        
        logger.error("Unexpected exception: ", ex);
        
        ApiResponse<Void> response = ApiResponse.error("UNEXPECTED_ERROR", 
            "An unexpected error occurred. Please contact support.");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}