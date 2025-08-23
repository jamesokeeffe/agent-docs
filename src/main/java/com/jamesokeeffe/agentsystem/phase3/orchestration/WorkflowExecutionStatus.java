package com.jamesokeeffe.agentsystem.phase3.orchestration;

/**
 * Enumeration of workflow execution status values.
 * 
 * Represents the current state of a workflow execution:
 * - PENDING: Execution is queued but not yet started
 * - RUNNING: Execution is currently in progress
 * - COMPLETED: Execution finished successfully
 * - FAILED: Execution failed with an error
 * - CANCELLED: Execution was cancelled before completion
 * - TIMEOUT: Execution exceeded maximum allowed time
 * 
 * @author James O'Keeffe
 * @version 3.0.0
 * @since 3.0.0
 */
public enum WorkflowExecutionStatus {
    
    /**
     * Execution is queued but not yet started.
     */
    PENDING("Pending"),
    
    /**
     * Execution is currently in progress.
     */
    RUNNING("Running"),
    
    /**
     * Execution finished successfully.
     */
    COMPLETED("Completed"),
    
    /**
     * Execution failed with an error.
     */
    FAILED("Failed"),
    
    /**
     * Execution was cancelled before completion.
     */
    CANCELLED("Cancelled"),
    
    /**
     * Execution exceeded maximum allowed time.
     */
    TIMEOUT("Timeout");

    private final String displayName;

    WorkflowExecutionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}