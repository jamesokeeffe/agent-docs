package com.jamesokeeffe.agentsystem.phase3.orchestration;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Workflow entity representing a complex multi-step process.
 * 
 * Workflows orchestrate multiple agents to complete complex tasks:
 * - Define step sequences and dependencies
 * - Handle agent coordination and communication
 * - Track execution progress and results
 * - Manage error handling and recovery
 * 
 * @author James O'Keeffe
 * @version 3.0.0
 * @since 3.0.0
 */
@Entity
@Table(name = "workflows", indexes = {
    @Index(name = "idx_workflow_name", columnList = "name"),
    @Index(name = "idx_workflow_status", columnList = "status"),
    @Index(name = "idx_workflow_created", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Workflow name is required")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Workflow definition is required")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String definition; // JSON workflow definition

    @NotNull(message = "Workflow status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStatus status;

    @Column(name = "execution_count")
    private Integer executionCount = 0;

    @Column(name = "success_count")
    private Integer successCount = 0;

    @Column(name = "failure_count")
    private Integer failureCount = 0;

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    @Column(name = "last_execution_duration_ms")
    private Long lastExecutionDurationMs;

    @Column(columnDefinition = "TEXT")
    private String metadata; // Additional workflow metadata

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Workflow() {}

    public Workflow(String name, String definition) {
        this.name = name;
        this.definition = definition;
        this.status = WorkflowStatus.DRAFT;
    }

    public Workflow(String name, String description, String definition) {
        this(name, definition);
        this.description = description;
    }

    // Business Methods
    
    /**
     * Checks if workflow is ready for execution.
     */
    public boolean isExecutable() {
        return status == WorkflowStatus.ACTIVE || status == WorkflowStatus.PAUSED;
    }

    /**
     * Activates the workflow for execution.
     */
    public void activate() {
        if (status == WorkflowStatus.DRAFT || status == WorkflowStatus.INACTIVE) {
            this.status = WorkflowStatus.ACTIVE;
        }
    }

    /**
     * Deactivates the workflow.
     */
    public void deactivate() {
        if (status == WorkflowStatus.ACTIVE || status == WorkflowStatus.PAUSED) {
            this.status = WorkflowStatus.INACTIVE;
        }
    }

    /**
     * Pauses workflow execution.
     */
    public void pause() {
        if (status == WorkflowStatus.ACTIVE) {
            this.status = WorkflowStatus.PAUSED;
        }
    }

    /**
     * Resumes workflow execution.
     */
    public void resume() {
        if (status == WorkflowStatus.PAUSED) {
            this.status = WorkflowStatus.ACTIVE;
        }
    }

    /**
     * Records workflow execution start.
     */
    public void recordExecutionStart() {
        this.lastExecutedAt = LocalDateTime.now();
        this.executionCount++;
    }

    /**
     * Records successful workflow execution.
     */
    public void recordExecutionSuccess(long durationMs) {
        this.successCount++;
        this.lastExecutionDurationMs = durationMs;
    }

    /**
     * Records failed workflow execution.
     */
    public void recordExecutionFailure(long durationMs) {
        this.failureCount++;
        this.lastExecutionDurationMs = durationMs;
    }

    /**
     * Gets workflow success rate.
     */
    public double getSuccessRate() {
        if (executionCount == 0) return 0.0;
        return (double) successCount / executionCount;
    }

    /**
     * Gets average execution duration.
     */
    public double getAverageExecutionTime() {
        return lastExecutionDurationMs != null ? lastExecutionDurationMs : 0.0;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public Integer getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(Integer executionCount) {
        this.executionCount = executionCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Integer failureCount) {
        this.failureCount = failureCount;
    }

    public LocalDateTime getLastExecutedAt() {
        return lastExecutedAt;
    }

    public void setLastExecutedAt(LocalDateTime lastExecutedAt) {
        this.lastExecutedAt = lastExecutedAt;
    }

    public Long getLastExecutionDurationMs() {
        return lastExecutionDurationMs;
    }

    public void setLastExecutionDurationMs(Long lastExecutionDurationMs) {
        this.lastExecutionDurationMs = lastExecutionDurationMs;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Workflow workflow = (Workflow) o;
        return Objects.equals(id, workflow.id) && Objects.equals(name, workflow.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Workflow{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", executionCount=" + executionCount +
                ", successRate=" + String.format("%.2f%%", getSuccessRate() * 100) +
                '}';
    }
}