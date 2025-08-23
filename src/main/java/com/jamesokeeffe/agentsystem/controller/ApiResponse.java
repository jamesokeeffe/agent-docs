package com.jamesokeeffe.agentsystem.controller;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Standard API response wrapper for consistent response format.
 * 
 * This class provides a consistent structure for all API responses,
 * including success/error status, data payload, error information,
 * and metadata.
 * 
 * @param <T> the type of data being returned
 * 
 * @author James O'Keeffe
 * @version 1.0.0
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String message;
    private ApiError error;
    private LocalDateTime timestamp;
    private String version;

    // Constructors
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
        this.version = "1.0.0";
    }

    public ApiResponse(boolean success, T data, String message) {
        this();
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public ApiResponse(boolean success, ApiError error) {
        this();
        this.success = success;
        this.error = error;
    }

    // Static factory methods for success responses
    
    /**
     * Creates a successful response with data and message.
     * 
     * @param data the response data
     * @param message the success message
     * @param <T> the data type
     * @return success response
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }

    /**
     * Creates a successful response with data only.
     * 
     * @param data the response data
     * @param <T> the data type
     * @return success response
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Operation completed successfully");
    }

    /**
     * Creates a successful response with message only.
     * 
     * @param message the success message
     * @return success response
     */
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, null, message);
    }

    // Static factory methods for error responses

    /**
     * Creates an error response with error code and message.
     * 
     * @param errorCode the error code
     * @param errorMessage the error message
     * @return error response
     */
    public static <T> ApiResponse<T> error(String errorCode, String errorMessage) {
        return new ApiResponse<>(false, new ApiError(errorCode, errorMessage));
    }

    /**
     * Creates an error response with error code, message, and details.
     * 
     * @param errorCode the error code
     * @param errorMessage the error message
     * @param details additional error details
     * @return error response
     */
    public static <T> ApiResponse<T> error(String errorCode, String errorMessage, Object details) {
        return new ApiResponse<>(false, new ApiError(errorCode, errorMessage, details));
    }

    /**
     * Creates an error response from an exception.
     * 
     * @param exception the exception
     * @return error response
     */
    public static <T> ApiResponse<T> error(Exception exception) {
        return error("INTERNAL_ERROR", exception.getMessage());
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ApiError getError() {
        return error;
    }

    public void setError(ApiError error) {
        this.error = error;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Inner class representing API error information.
     */
    public static class ApiError {
        private String code;
        private String message;
        private Object details;

        public ApiError() {}

        public ApiError(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public ApiError(String code, String message, Object details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getDetails() {
            return details;
        }

        public void setDetails(Object details) {
            this.details = details;
        }

        @Override
        public String toString() {
            return "ApiError{" +
                    "code='" + code + '\'' +
                    ", message='" + message + '\'' +
                    ", details=" + details +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", error=" + error +
                ", timestamp=" + timestamp +
                '}';
    }
}