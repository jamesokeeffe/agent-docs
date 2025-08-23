package com.jamesokeeffe.agentsystem.phase2.plugin;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Result object returned by plugin execution.
 * 
 * Contains:
 * - Execution status and results
 * - Output data
 * - Error information
 * - Execution metrics
 * 
 * @author James O'Keeffe
 * @version 2.0.0
 * @since 2.0.0
 */
public class PluginResult {

    private final boolean success;
    private final String message;
    private final Map<String, Object> data;
    private final Throwable error;
    private final LocalDateTime executedAt;
    private final long executionTimeMs;

    private PluginResult(Builder builder) {
        this.success = builder.success;
        this.message = builder.message;
        this.data = builder.data != null ? new HashMap<>(builder.data) : new HashMap<>();
        this.error = builder.error;
        this.executedAt = builder.executedAt != null ? builder.executedAt : LocalDateTime.now();
        this.executionTimeMs = builder.executionTimeMs;
    }

    // Static factory methods
    public static PluginResult success(String message) {
        return new Builder(true).message(message).build();
    }

    public static PluginResult success(String message, Map<String, Object> data) {
        return new Builder(true).message(message).data(data).build();
    }

    public static PluginResult failure(String message) {
        return new Builder(false).message(message).build();
    }

    public static PluginResult failure(String message, Throwable error) {
        return new Builder(false).message(message).error(error).build();
    }

    public static Builder builder() {
        return new Builder(true);
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Throwable getError() {
        return error;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    // Utility methods
    public Object getData(String key) {
        return data.get(key);
    }

    public <T> T getData(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    // Builder class
    public static class Builder {
        private boolean success;
        private String message;
        private Map<String, Object> data;
        private Throwable error;
        private LocalDateTime executedAt;
        private long executionTimeMs;

        public Builder(boolean success) {
            this.success = success;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        public Builder data(String key, Object value) {
            if (this.data == null) {
                this.data = new HashMap<>();
            }
            this.data.put(key, value);
            return this;
        }

        public Builder error(Throwable error) {
            this.error = error;
            return this;
        }

        public Builder executedAt(LocalDateTime executedAt) {
            this.executedAt = executedAt;
            return this;
        }

        public Builder executionTime(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }

        public PluginResult build() {
            return new PluginResult(this);
        }
    }

    @Override
    public String toString() {
        return "PluginResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", dataKeys=" + (data != null ? data.keySet() : "[]") +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
}