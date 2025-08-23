package com.jamesokeeffe.agentsystem.phase4.mcp;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Result of MCP tool execution.
 * 
 * @author James O'Keeffe
 * @version 4.0.0
 * @since 4.0.0
 */
public class MCPExecutionResult {

    private final boolean success;
    private final Map<String, Object> result;
    private final String error;
    private final long executionTimeMs;
    private final LocalDateTime timestamp;

    private MCPExecutionResult(boolean success, Map<String, Object> result, String error, long executionTimeMs) {
        this.success = success;
        this.result = result;
        this.error = error;
        this.executionTimeMs = executionTimeMs;
        this.timestamp = LocalDateTime.now();
    }

    public static MCPExecutionResult success(Map<String, Object> result, long executionTimeMs) {
        return new MCPExecutionResult(true, result, null, executionTimeMs);
    }

    public static MCPExecutionResult failure(String error) {
        return new MCPExecutionResult(false, null, error, 0);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public Map<String, Object> getResult() { return result; }
    public String getError() { return error; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public LocalDateTime getTimestamp() { return timestamp; }
}