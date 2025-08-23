package com.jamesokeeffe.agentsystem.phase3.orchestration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Repository interface for WorkflowExecution entity operations.
 * 
 * Provides data access methods for workflow execution tracking:
 * - Basic CRUD operations
 * - Queries by workflow and status
 * - Execution history and metrics
 * 
 * @author James O'Keeffe
 * @version 3.0.0
 * @since 3.0.0
 */
@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, Long> {

    /**
     * Find executions for a specific workflow ordered by start time.
     */
    List<WorkflowExecution> findByWorkflowIdOrderByStartedAtDesc(Long workflowId);

    /**
     * Find executions by status.
     */
    List<WorkflowExecution> findByStatus(WorkflowExecutionStatus status);

    /**
     * Find executions by workflow and status.
     */
    List<WorkflowExecution> findByWorkflowIdAndStatus(Long workflowId, WorkflowExecutionStatus status);

    /**
     * Find running executions.
     */
    @Query("SELECT e FROM WorkflowExecution e WHERE e.status = 'RUNNING'")
    List<WorkflowExecution> findRunningExecutions();

    /**
     * Find executions started after a specific date.
     */
    List<WorkflowExecution> findByStartedAtAfter(LocalDateTime date);

    /**
     * Find recent executions for a workflow.
     */
    List<WorkflowExecution> findTop10ByWorkflowIdOrderByStartedAtDesc(Long workflowId);

    /**
     * Count executions by workflow.
     */
    @Query("SELECT COUNT(e) FROM WorkflowExecution e WHERE e.workflowId = :workflowId")
    Long countByWorkflowId(@Param("workflowId") Long workflowId);

    /**
     * Count executions by workflow and status.
     */
    @Query("SELECT COUNT(e) FROM WorkflowExecution e WHERE e.workflowId = :workflowId AND e.status = :status")
    Long countByWorkflowIdAndStatus(@Param("workflowId") Long workflowId, @Param("status") WorkflowExecutionStatus status);

    /**
     * Find long-running executions.
     */
    @Query("SELECT e FROM WorkflowExecution e WHERE e.status = 'RUNNING' AND e.startedAt < :threshold")
    List<WorkflowExecution> findLongRunningExecutions(@Param("threshold") LocalDateTime threshold);

    /**
     * Find executions with duration greater than threshold.
     */
    @Query("SELECT e FROM WorkflowExecution e WHERE e.durationMs > :thresholdMs")
    List<WorkflowExecution> findSlowExecutions(@Param("thresholdMs") long thresholdMs);

    /**
     * Get average execution time for a workflow.
     */
    @Query("SELECT AVG(e.durationMs) FROM WorkflowExecution e WHERE e.workflowId = :workflowId AND e.durationMs IS NOT NULL")
    Double getAverageExecutionTime(@Param("workflowId") Long workflowId);

    /**
     * Find failed executions with error details.
     */
    @Query("SELECT e FROM WorkflowExecution e WHERE e.status = 'FAILED' AND e.errorMessage IS NOT NULL")
    List<WorkflowExecution> findFailedExecutionsWithErrors();

    /**
     * Get execution statistics for a workflow.
     */
    @Query("SELECT new map(" +
           "COUNT(e) as totalExecutions, " +
           "SUM(CASE WHEN e.status = 'COMPLETED' THEN 1 ELSE 0 END) as successfulExecutions, " +
           "SUM(CASE WHEN e.status = 'FAILED' THEN 1 ELSE 0 END) as failedExecutions, " +
           "AVG(e.durationMs) as averageDuration" +
           ") FROM WorkflowExecution e WHERE e.workflowId = :workflowId")
    List<Map<String, Object>> getExecutionStatistics(@Param("workflowId") Long workflowId);

    /**
     * Delete old completed executions.
     */
    @Query("DELETE FROM WorkflowExecution e WHERE e.status IN ('COMPLETED', 'FAILED', 'CANCELLED') AND e.completedAt < :cutoffDate")
    void deleteOldExecutions(@Param("cutoffDate") LocalDateTime cutoffDate);
}