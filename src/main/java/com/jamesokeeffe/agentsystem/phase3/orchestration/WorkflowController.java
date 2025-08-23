package com.jamesokeeffe.agentsystem.phase3.orchestration;

import com.jamesokeeffe.agentsystem.controller.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for workflow management and execution.
 * 
 * Provides endpoints for:
 * - Workflow CRUD operations
 * - Workflow execution and monitoring
 * - Execution history and status
 * - Workflow statistics and metrics
 * 
 * @author James O'Keeffe
 * @version 3.0.0
 * @since 3.0.0
 */
@RestController
@RequestMapping("/api/v1/workflows")
@CrossOrigin(origins = "*")
public class WorkflowController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowController.class);

    private final WorkflowOrchestrator workflowOrchestrator;

    @Autowired
    public WorkflowController(WorkflowOrchestrator workflowOrchestrator) {
        this.workflowOrchestrator = workflowOrchestrator;
    }

    /**
     * Get all workflows.
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<Workflow>>> getAllWorkflows() {
        try {
            List<Workflow> workflows = workflowOrchestrator.getAllWorkflows();
            logger.debug("Retrieved {} workflows", workflows.size());
            return ResponseEntity.ok(ApiResponse.success(workflows));
        } catch (Exception e) {
            logger.error("Error retrieving workflows: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("WORKFLOW_RETRIEVAL_ERROR", "Failed to retrieve workflows"));
        }
    }

    /**
     * Get workflow by ID.
     */
    @GetMapping("/{workflowId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Workflow>> getWorkflow(@PathVariable Long workflowId) {
        try {
            Workflow workflow = workflowOrchestrator.getWorkflow(workflowId);
            if (workflow == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success(workflow));
        } catch (Exception e) {
            logger.error("Error retrieving workflow {}: {}", workflowId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("WORKFLOW_RETRIEVAL_ERROR", "Failed to retrieve workflow"));
        }
    }

    /**
     * Create a new workflow.
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Workflow>> createWorkflow(@Valid @RequestBody CreateWorkflowRequest request) {
        try {
            Workflow workflow = workflowOrchestrator.createWorkflow(
                request.getName(),
                request.getDescription(),
                request.getDefinition()
            );
            
            logger.info("Created workflow: {}", workflow.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(workflow));
        } catch (Exception e) {
            logger.error("Error creating workflow: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("WORKFLOW_CREATION_ERROR", "Failed to create workflow"));
        }
    }

    /**
     * Execute a workflow.
     */
    @PostMapping("/{workflowId}/execute")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<WorkflowExecutionResponse>> executeWorkflow(
            @PathVariable Long workflowId,
            @RequestBody(required = false) Map<String, Object> input) {
        try {
            if (input == null) {
                input = Map.of();
            }
            
            CompletableFuture<WorkflowExecution> future = workflowOrchestrator.executeWorkflow(workflowId, input);
            
            // Get the execution ID immediately (this will be available right away)
            WorkflowExecution execution = future.getNow(null);
            if (execution == null) {
                // If not available immediately, we need to handle this differently
                WorkflowExecutionResponse response = new WorkflowExecutionResponse();
                response.setMessage("Workflow execution started");
                response.setStatus("PENDING");
                
                return ResponseEntity.accepted()
                    .body(ApiResponse.success(response));
            } else {
                WorkflowExecutionResponse response = new WorkflowExecutionResponse();
                response.setExecutionId(execution.getId());
                response.setWorkflowId(workflowId);
                response.setStatus(execution.getStatus().toString());
                response.setMessage("Workflow execution started");
                
                logger.info("Started workflow execution {} for workflow {}", execution.getId(), workflowId);
                return ResponseEntity.accepted()
                    .body(ApiResponse.success(response));
            }
        } catch (Exception e) {
            logger.error("Error executing workflow {}: {}", workflowId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("WORKFLOW_EXECUTION_ERROR", "Failed to execute workflow"));
        }
    }

    /**
     * Get workflow executions.
     */
    @GetMapping("/{workflowId}/executions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<WorkflowExecution>>> getWorkflowExecutions(@PathVariable Long workflowId) {
        try {
            List<WorkflowExecution> executions = workflowOrchestrator.getWorkflowExecutions(workflowId);
            logger.debug("Retrieved {} executions for workflow {}", executions.size(), workflowId);
            return ResponseEntity.ok(ApiResponse.success(executions));
        } catch (Exception e) {
            logger.error("Error retrieving executions for workflow {}: {}", workflowId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("EXECUTION_RETRIEVAL_ERROR", "Failed to retrieve executions"));
        }
    }

    /**
     * Get execution by ID.
     */
    @GetMapping("/executions/{executionId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<WorkflowExecution>> getExecution(@PathVariable Long executionId) {
        try {
            WorkflowExecution execution = workflowOrchestrator.getExecution(executionId);
            if (execution == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success(execution));
        } catch (Exception e) {
            logger.error("Error retrieving execution {}: {}", executionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("EXECUTION_RETRIEVAL_ERROR", "Failed to retrieve execution"));
        }
    }

    /**
     * Cancel a workflow execution.
     */
    @PostMapping("/executions/{executionId}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> cancelExecution(@PathVariable Long executionId) {
        try {
            boolean cancelled = workflowOrchestrator.cancelExecution(executionId);
            if (cancelled) {
                logger.info("Cancelled workflow execution {}", executionId);
                return ResponseEntity.ok(ApiResponse.success("Execution cancelled successfully"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("CANCELLATION_FAILED", "Could not cancel execution"));
            }
        } catch (Exception e) {
            logger.error("Error cancelling execution {}: {}", executionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("CANCELLATION_ERROR", "Failed to cancel execution"));
        }
    }

    /**
     * Get system workflow statistics.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WorkflowStats>> getWorkflowStats() {
        try {
            WorkflowStats stats = new WorkflowStats(
                workflowOrchestrator.getAllWorkflows().size(),
                workflowOrchestrator.getRunningExecutionCount()
            );
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            logger.error("Error retrieving workflow stats: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("STATS_ERROR", "Failed to retrieve workflow statistics"));
        }
    }

    // DTOs
    public static class CreateWorkflowRequest {
        private String name;
        private String description;
        private String definition;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }
    }

    public static class WorkflowExecutionResponse {
        private Long executionId;
        private Long workflowId;
        private String status;
        private String message;

        // Getters and setters
        public Long getExecutionId() { return executionId; }
        public void setExecutionId(Long executionId) { this.executionId = executionId; }
        public Long getWorkflowId() { return workflowId; }
        public void setWorkflowId(Long workflowId) { this.workflowId = workflowId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class WorkflowStats {
        private final int totalWorkflows;
        private final int runningExecutions;

        public WorkflowStats(int totalWorkflows, int runningExecutions) {
            this.totalWorkflows = totalWorkflows;
            this.runningExecutions = runningExecutions;
        }

        public int getTotalWorkflows() { return totalWorkflows; }
        public int getRunningExecutions() { return runningExecutions; }
    }
}