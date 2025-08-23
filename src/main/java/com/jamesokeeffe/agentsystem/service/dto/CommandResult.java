package com.jamesokeeffe.agentsystem.service.dto;

import com.jamesokeeffe.agentsystem.model.CommandStatus;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for command execution results.
 * 
 * This DTO is returned after a command execution attempt, containing
 * the result, status, and execution metadata.
 * 
 * @author James O'Keeffe
 * @version 1.0.0
 * @since 1.0.0
 */
public class CommandResult {

    private Long commandId;
    private CommandStatus status;
    private String result;
    private String errorMessage;
    private Long executionTimeMs;
    private LocalDateTime timestamp;
    private boolean success;

    // Constructors
    public CommandResult() {}

    public CommandResult(CommandStatus status, String result) {
        this.status = status;
        this.result = result;
        this.success = status == CommandStatus.COMPLETED;
        this.timestamp = LocalDateTime.now();
    }

    // Static factory methods for common scenarios
    public static CommandResult success(String result) {
        return new CommandResult(CommandStatus.COMPLETED, result);
    }

    public static CommandResult failure(String errorMessage) {
        CommandResult result = new CommandResult();
        result.status = CommandStatus.FAILED;
        result.errorMessage = errorMessage;
        result.success = false;
        result.timestamp = LocalDateTime.now();
        return result;
    }

    public static CommandResult pending() {
        CommandResult result = new CommandResult();
        result.status = CommandStatus.PENDING;
        result.success = false;
        result.timestamp = LocalDateTime.now();
        return result;
    }

    public static CommandResult executing() {
        CommandResult result = new CommandResult();
        result.status = CommandStatus.EXECUTING;
        result.success = false;
        result.timestamp = LocalDateTime.now();
        return result;
    }

    // Getters and Setters
    public Long getCommandId() {
        return commandId;
    }

    public void setCommandId(Long commandId) {
        this.commandId = commandId;
    }

    public CommandStatus getStatus() {
        return status;
    }

    public void setStatus(CommandStatus status) {
        this.status = status;
        this.success = status == CommandStatus.COMPLETED;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "CommandResult{" +
                "commandId=" + commandId +
                ", status=" + status +
                ", success=" + success +
                ", executionTimeMs=" + executionTimeMs +
                ", timestamp=" + timestamp +
                '}';
    }
}