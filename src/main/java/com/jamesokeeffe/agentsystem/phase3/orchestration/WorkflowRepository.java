package com.jamesokeeffe.agentsystem.phase3.orchestration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Repository interface for Workflow entity operations.
 * 
 * Provides data access methods for workflow management:
 * - Basic CRUD operations
 * - Queries by status and execution metrics
 * - Workflow ordering and filtering
 * 
 * @author James O'Keeffe
 * @version 3.0.0
 * @since 3.0.0
 */
@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

    /**
     * Find workflows by status.
     */
    List<Workflow> findByStatus(WorkflowStatus status);

    /**
     * Find workflows by name (case-insensitive).
     */
    List<Workflow> findByNameContainingIgnoreCase(String name);

    /**
     * Find active workflows available for execution.
     */
    @Query("SELECT w FROM Workflow w WHERE w.status = 'ACTIVE'")
    List<Workflow> findActiveWorkflows();

    /**
     * Find workflows created after a specific date.
     */
    List<Workflow> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find workflows ordered by execution count.
     */
    List<Workflow> findAllByOrderByExecutionCountDesc();

    /**
     * Find workflows with high success rate.
     */
    @Query("SELECT w FROM Workflow w WHERE w.executionCount > 0 AND (w.successCount * 100.0 / w.executionCount) >= :minSuccessRate")
    List<Workflow> findBySuccessRateGreaterThan(@Param("minSuccessRate") double minSuccessRate);

    /**
     * Find workflows executed recently.
     */
    List<Workflow> findByLastExecutedAtAfter(LocalDateTime date);

    /**
     * Count workflows by status.
     */
    @Query("SELECT COUNT(w) FROM Workflow w WHERE w.status = :status")
    Long countByStatus(@Param("status") WorkflowStatus status);

    /**
     * Find workflows with execution count greater than threshold.
     */
    @Query("SELECT w FROM Workflow w WHERE w.executionCount > :threshold")
    List<Workflow> findByExecutionCountGreaterThan(@Param("threshold") int threshold);

    /**
     * Find workflows never executed.
     */
    @Query("SELECT w FROM Workflow w WHERE w.executionCount = 0 OR w.lastExecutedAt IS NULL")
    List<Workflow> findNeverExecutedWorkflows();

    /**
     * Get workflow statistics.
     */
    @Query("SELECT new map(" +
           "COUNT(w) as totalWorkflows, " +
           "SUM(w.executionCount) as totalExecutions, " +
           "SUM(w.successCount) as totalSuccesses, " +
           "SUM(w.failureCount) as totalFailures" +
           ") FROM Workflow w")
    List<Map<String, Object>> getWorkflowStatistics();
}