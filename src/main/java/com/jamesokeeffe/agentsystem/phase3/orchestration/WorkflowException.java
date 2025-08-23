package com.jamesokeeffe.agentsystem.phase3.orchestration;

/**
 * Exception thrown by workflow operations.
 * 
 * @author James O'Keeffe
 * @version 3.0.0
 * @since 3.0.0
 */
public class WorkflowException extends Exception {

    private final String workflowName;
    private final String stepName;
    private final WorkflowErrorCode errorCode;

    public WorkflowException(String message) {
        super(message);
        this.workflowName = null;
        this.stepName = null;
        this.errorCode = WorkflowErrorCode.UNKNOWN;
    }

    public WorkflowException(String message, Throwable cause) {
        super(message, cause);
        this.workflowName = null;
        this.stepName = null;
        this.errorCode = WorkflowErrorCode.UNKNOWN;
    }

    public WorkflowException(String workflowName, WorkflowErrorCode errorCode, String message) {
        super(message);
        this.workflowName = workflowName;
        this.stepName = null;
        this.errorCode = errorCode;
    }

    public WorkflowException(String workflowName, String stepName, WorkflowErrorCode errorCode, String message) {
        super(message);
        this.workflowName = workflowName;
        this.stepName = stepName;
        this.errorCode = errorCode;
    }

    public WorkflowException(String workflowName, WorkflowErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.workflowName = workflowName;
        this.stepName = null;
        this.errorCode = errorCode;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public String getStepName() {
        return stepName;
    }

    public WorkflowErrorCode getErrorCode() {
        return errorCode;
    }

    public enum WorkflowErrorCode {
        DEFINITION_INVALID,
        STEP_FAILED,
        AGENT_NOT_FOUND,
        TIMEOUT,
        DEPENDENCY_FAILED,
        CONDITION_FAILED,
        EXECUTION_CANCELLED,
        RESOURCE_UNAVAILABLE,
        UNKNOWN
    }
}