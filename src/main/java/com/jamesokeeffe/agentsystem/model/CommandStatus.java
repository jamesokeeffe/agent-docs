package com.jamesokeeffe.agentsystem.model;

/**
 * Enumeration of possible command execution statuses.
 * 
 * Command status tracks the lifecycle of command execution:
 * 
 * - PENDING: Command has been received but not yet started
 * - EXECUTING: Command is currently being processed
 * - COMPLETED: Command executed successfully
 * - FAILED: Command execution failed with an error
 * 
 * @author James O'Keeffe
 * @version 1.0.0
 * @since 1.0.0
 */
public enum CommandStatus {
    
    /**
     * Command has been received and is waiting to be executed.
     * This is the initial state when a command is first created.
     */
    PENDING("Pending", "Command is waiting to be executed"),
    
    /**
     * Command is currently being executed by an agent.
     * The agent status should be BUSY during this state.
     */
    EXECUTING("Executing", "Command is being processed"),
    
    /**
     * Command executed successfully and produced a result.
     * The agent should return to IDLE status after completion.
     */
    COMPLETED("Completed", "Command executed successfully"),
    
    /**
     * Command execution failed due to an error.
     * Error details should be stored in the errorMessage field.
     */
    FAILED("Failed", "Command execution failed");

    private final String displayName;
    private final String description;

    CommandStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets the human-readable display name for this status.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the description of what this status means.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this status indicates the command is still being processed.
     * 
     * @return true if the command is in progress
     */
    public boolean isInProgress() {
        return this == PENDING || this == EXECUTING;
    }

    /**
     * Checks if this status indicates the command has finished (success or failure).
     * 
     * @return true if the command is finished
     */
    public boolean isFinished() {
        return this == COMPLETED || this == FAILED;
    }

    /**
     * Checks if this status indicates successful completion.
     * 
     * @return true if the command completed successfully
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }

    /**
     * Gets all statuses that indicate a command is still active.
     * 
     * @return array of active statuses
     */
    public static CommandStatus[] getActiveStatuses() {
        return new CommandStatus[]{PENDING, EXECUTING};
    }

    /**
     * Gets all statuses that indicate a command has finished.
     * 
     * @return array of finished statuses
     */
    public static CommandStatus[] getFinishedStatuses() {
        return new CommandStatus[]{COMPLETED, FAILED};
    }
}