package com.jamesokeeffe.agentsystem.phase3.orchestration;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * WorkflowExecution entity representing a single execution instance of a workflow.
 * 
 * Tracks:
 * - Execution status and progress
 * - Results and outputs
 * - Timing and performance metrics
 * - Error information and debugging data
 * 
 * @author James O'Keeffe
 * @version 3.0.0
 * @since 3.0.0
 */
@Entity
@Table(name = "workflow_executions", indexes = {
    @Index(name = "idx_execution_workflow", columnList = "workflow_id"),
    @Index(name = "idx_execution_status", columnList = "status"),
    @Index(name = "idx_execution_started", columnList = "started_at")
})
@EntityListeners(AuditingEntityListener.class)
public class WorkflowExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Workflow ID is required")
    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    @NotNull(message = "Execution status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowExecutionStatus status;

    @Column(columnDefinition = "TEXT")
    private String input; // JSON input parameters

    @Column(columnDefinition = "TEXT")
    private String output; // JSON output results

    @Column(columnDefinition = "TEXT")
    private String context; // Execution context and state

    @Column(name = "current_step")
    private String currentStep;

    @Column(name = "total_steps")
    private Integer totalSteps;

    @Column(name = "completed_steps")
    private Integer completedSteps = 0;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String errorDetails;

    @CreatedDate
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    // Constructors
    public WorkflowExecution() {}

    public WorkflowExecution(Long workflowId, String input) {
        this.workflowId = workflowId;
        this.input = input;
        this.status = WorkflowExecutionStatus.PENDING;
    }

    // Business Methods
    
    /**
     * Starts the workflow execution.
     */
    public void start() {
        if (status == WorkflowExecutionStatus.PENDING) {
            this.status = WorkflowExecutionStatus.RUNNING;
            this.startedAt = LocalDateTime.now();
        }
    }

    /**
     * Completes the workflow execution successfully.
     */
    public void complete(String output) {
        if (status == WorkflowExecutionStatus.RUNNING) {
            this.status = WorkflowExecutionStatus.COMPLETED;
            this.output = output;
            this.completedAt = LocalDateTime.now();
            calculateDuration();
        }
    }

    /**
     * Fails the workflow execution.
     */
    public void fail(String errorMessage, String errorDetails) {
        if (status == WorkflowExecutionStatus.RUNNING || status == WorkflowExecutionStatus.PENDING) {
            this.status = WorkflowExecutionStatus.FAILED;
            this.errorMessage = errorMessage;
            this.errorDetails = errorDetails;
            this.completedAt = LocalDateTime.now();
            calculateDuration();
        }
    }

    /**
     * Cancels the workflow execution.
     */
    public void cancel() {
        if (status == WorkflowExecutionStatus.RUNNING || status == WorkflowExecutionStatus.PENDING) {
            this.status = WorkflowExecutionStatus.CANCELLED;
            this.completedAt = LocalDateTime.now();
            calculateDuration();
        }
    }

    /**
     * Updates execution progress.
     */
    public void updateProgress(String currentStep, int completedSteps, int totalSteps) {
        this.currentStep = currentStep;
        this.completedSteps = completedSteps;
        this.totalSteps = totalSteps;
    }

    /**
     * Calculates execution duration.
     */
    private void calculateDuration() {
        if (startedAt != null && completedAt != null) {
            this.durationMs = java.time.Duration.between(startedAt, completedAt).toMillis();
        }
    }

    /**
     * Gets execution progress percentage.
     */
    public double getProgressPercentage() {
        if (totalSteps == null || totalSteps == 0) return 0.0;
        return (double) completedSteps / totalSteps * 100.0;
    }

    /**
     * Checks if execution is finished.
     */
    public boolean isFinished() {
        return status == WorkflowExecutionStatus.COMPLETED ||
               status == WorkflowExecutionStatus.FAILED ||
               status == WorkflowExecutionStatus.CANCELLED;
    }

    /**
     * Checks if execution was successful.
     */
    public boolean isSuccessful() {
        return status == WorkflowExecutionStatus.COMPLETED;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    public WorkflowExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowExecutionStatus status) {
        this.status = status;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public Integer getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(Integer totalSteps) {
        this.totalSteps = totalSteps;
    }

    public Integer getCompletedSteps() {
        return completedSteps;
    }

    public void setCompletedSteps(Integer completedSteps) {
        this.completedSteps = completedSteps;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowExecution that = (WorkflowExecution) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "WorkflowExecution{" +
                "id=" + id +
                ", workflowId=" + workflowId +
                ", status=" + status +
                ", currentStep='" + currentStep + '\'' +
                ", progress=" + String.format("%.1f%%", getProgressPercentage()) +
                ", duration=" + (durationMs != null ? durationMs + "ms" : "N/A") +
                '}';
    }
}