package com.jamesokeeffe.agentsystem.phase3.orchestration;

/**
 * Enumeration of workflow status values.
 * 
 * Represents the current state of a workflow:
 * - DRAFT: Workflow is being designed and not ready for execution
 * - ACTIVE: Workflow is ready and available for execution
 * - PAUSED: Workflow execution is temporarily suspended
 * - INACTIVE: Workflow is disabled and not available for execution
 * - ARCHIVED: Workflow is archived and read-only
 * 
 * @author James O'Keeffe
 * @version 3.0.0
 * @since 3.0.0
 */
public enum WorkflowStatus {
    
    /**
     * Workflow is in draft state, not ready for execution.
     */
    DRAFT("Draft"),
    
    /**
     * Workflow is active and ready for execution.
     */
    ACTIVE("Active"),
    
    /**
     * Workflow execution is temporarily paused.
     */
    PAUSED("Paused"),
    
    /**
     * Workflow is inactive and not available for execution.
     */
    INACTIVE("Inactive"),
    
    /**
     * Workflow is archived and read-only.
     */
    ARCHIVED("Archived");

    private final String displayName;

    WorkflowStatus(String displayName) {
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