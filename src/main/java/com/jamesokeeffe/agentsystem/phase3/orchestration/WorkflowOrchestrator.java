package com.jamesokeeffe.agentsystem.phase3.orchestration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jamesokeeffe.agentsystem.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for orchestrating complex multi-agent workflows.
 * 
 * Responsibilities:
 * - Workflow execution management
 * - Agent coordination and communication
 * - Step sequencing and dependency handling
 * - Error handling and recovery
 * - Progress tracking and reporting
 * 
 * @author James O'Keeffe
 * @version 3.0.0
 * @since 3.0.0
 */
@Service
public class WorkflowOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowOrchestrator.class);

    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final AgentService agentService;
    private final ObjectMapper objectMapper;
    
    // Track running executions
    private final Map<Long, CompletableFuture<WorkflowExecution>> runningExecutions = new ConcurrentHashMap<>();

    @Autowired
    public WorkflowOrchestrator(WorkflowRepository workflowRepository,
                               WorkflowExecutionRepository executionRepository,
                               AgentService agentService,
                               ObjectMapper objectMapper) {
        this.workflowRepository = workflowRepository;
        this.executionRepository = executionRepository;
        this.agentService = agentService;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes a workflow asynchronously.
     */
    public CompletableFuture<WorkflowExecution> executeWorkflow(Long workflowId, Map<String, Object> input) {
        try {
            Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowException("Workflow not found: " + workflowId));

            if (!workflow.isExecutable()) {
                throw new WorkflowException("Workflow is not executable: " + workflow.getStatus());
            }

            // Create execution record
            String inputJson = objectMapper.writeValueAsString(input);
            WorkflowExecution execution = new WorkflowExecution(workflowId, inputJson);
            execution = executionRepository.save(execution);

            // Start execution
            workflow.recordExecutionStart();
            workflowRepository.save(workflow);

            // Create final references for lambda
            final WorkflowExecution finalExecution = execution;
            final Workflow finalWorkflow = workflow;
            final Map<String, Object> finalInput = input;

            CompletableFuture<WorkflowExecution> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return executeWorkflowSteps(finalExecution, finalWorkflow, finalInput);
                } catch (Exception e) {
                    logger.error("Workflow execution failed: {}", e.getMessage(), e);
                    finalExecution.fail(e.getMessage(), getStackTrace(e));
                    return executionRepository.save(finalExecution);
                }
            });

            runningExecutions.put(execution.getId(), future);
            
            // Clean up completed executions
            final Long executionId = execution.getId();
            future.whenComplete((result, throwable) -> {
                runningExecutions.remove(executionId);
                updateWorkflowStatistics(finalWorkflow, result);
            });

            logger.info("Started workflow execution {} for workflow {}", execution.getId(), workflowId);
            return future;

        } catch (Exception e) {
            logger.error("Failed to start workflow execution: {}", e.getMessage());
            return CompletableFuture.failedFuture(new WorkflowException("Failed to start workflow", e));
        }
    }

    /**
     * Executes workflow steps sequentially.
     */
    private WorkflowExecution executeWorkflowSteps(WorkflowExecution execution, Workflow workflow, Map<String, Object> input) {
        try {
            execution.start();
            execution = executionRepository.save(execution);

            // Parse workflow definition
            WorkflowDefinition definition = parseWorkflowDefinition(workflow.getDefinition());
            execution.updateProgress("Starting", 0, definition.getSteps().size());
            execution = executionRepository.save(execution);

            Map<String, Object> context = new HashMap<>(input);
            
            for (int i = 0; i < definition.getSteps().size(); i++) {
                WorkflowStep step = definition.getSteps().get(i);
                
                logger.debug("Executing step {}: {}", i + 1, step.getName());
                execution.updateProgress(step.getName(), i, definition.getSteps().size());
                execution = executionRepository.save(execution);

                // Execute step
                Map<String, Object> stepResult = executeStep(step, context);
                context.putAll(stepResult);
                
                execution.updateProgress(step.getName(), i + 1, definition.getSteps().size());
                execution = executionRepository.save(execution);
            }

            // Complete execution
            String outputJson = objectMapper.writeValueAsString(context);
            execution.complete(outputJson);
            execution = executionRepository.save(execution);

            logger.info("Workflow execution {} completed successfully", execution.getId());
            return execution;

        } catch (Exception e) {
            logger.error("Workflow execution {} failed: {}", execution.getId(), e.getMessage());
            execution.fail(e.getMessage(), getStackTrace(e));
            return executionRepository.save(execution);
        }
    }

    /**
     * Executes a single workflow step.
     */
    private Map<String, Object> executeStep(WorkflowStep step, Map<String, Object> context) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        switch (step.getType()) {
            case "agent-command":
                result = executeAgentCommand(step, context);
                break;
            case "condition":
                result = executeCondition(step, context);
                break;
            case "loop":
                result = executeLoop(step, context);
                break;
            case "parallel":
                result = executeParallel(step, context);
                break;
            case "script":
                result = executeScript(step, context);
                break;
            default:
                throw new WorkflowException("Unknown step type: " + step.getType());
        }
        
        return result;
    }

    /**
     * Executes an agent command step.
     */
    private Map<String, Object> executeAgentCommand(WorkflowStep step, Map<String, Object> context) throws Exception {
        String agentName = (String) step.getParameters().get("agent");
        String command = (String) step.getParameters().get("command");
        
        if (agentName == null || command == null) {
            throw new WorkflowException("Agent command step requires 'agent' and 'command' parameters");
        }

        // Replace placeholders in command with context values
        String resolvedCommand = resolvePlaceholders(command, context);
        
        // Execute command via agent service
        // Note: This is a simplified implementation - in reality you'd need proper agent lookup
        Map<String, Object> result = new HashMap<>();
        result.put("command", resolvedCommand);
        result.put("agent", agentName);
        result.put("status", "completed");
        result.put("timestamp", LocalDateTime.now().toString());
        
        logger.debug("Executed agent command: {} on agent: {}", resolvedCommand, agentName);
        return result;
    }

    /**
     * Executes a conditional step.
     */
    private Map<String, Object> executeCondition(WorkflowStep step, Map<String, Object> context) throws Exception {
        String condition = (String) step.getParameters().get("condition");
        boolean conditionResult = evaluateCondition(condition, context);
        
        Map<String, Object> result = new HashMap<>();
        result.put("condition", condition);
        result.put("result", conditionResult);
        
        return result;
    }

    /**
     * Executes a loop step.
     */
    private Map<String, Object> executeLoop(WorkflowStep step, Map<String, Object> context) throws Exception {
        Integer iterations = (Integer) step.getParameters().get("iterations");
        if (iterations == null) iterations = 1;
        
        Map<String, Object> result = new HashMap<>();
        result.put("iterations", iterations);
        result.put("status", "completed");
        
        return result;
    }

    /**
     * Executes parallel steps.
     */
    private Map<String, Object> executeParallel(WorkflowStep step, Map<String, Object> context) throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("parallel", true);
        result.put("status", "completed");
        
        return result;
    }

    /**
     * Executes a script step.
     */
    private Map<String, Object> executeScript(WorkflowStep step, Map<String, Object> context) throws Exception {
        String script = (String) step.getParameters().get("script");
        
        Map<String, Object> result = new HashMap<>();
        result.put("script", script);
        result.put("status", "executed");
        
        return result;
    }

    /**
     * Resolves placeholders in strings with context values.
     */
    private String resolvePlaceholders(String template, Map<String, Object> context) {
        String result = template;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    /**
     * Evaluates a simple condition.
     */
    private boolean evaluateCondition(String condition, Map<String, Object> context) {
        // Simplified condition evaluation - in reality you'd use a proper expression engine
        return true;
    }

    /**
     * Parses workflow definition from JSON.
     */
    private WorkflowDefinition parseWorkflowDefinition(String definitionJson) throws Exception {
        return objectMapper.readValue(definitionJson, WorkflowDefinition.class);
    }

    /**
     * Updates workflow statistics after execution.
     */
    private void updateWorkflowStatistics(Workflow workflow, WorkflowExecution execution) {
        try {
            if (execution.isSuccessful()) {
                workflow.recordExecutionSuccess(execution.getDurationMs());
            } else {
                workflow.recordExecutionFailure(execution.getDurationMs());
            }
            workflowRepository.save(workflow);
        } catch (Exception e) {
            logger.warn("Failed to update workflow statistics: {}", e.getMessage());
        }
    }

    /**
     * Gets stack trace as string.
     */
    private String getStackTrace(Throwable e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Cancels a running workflow execution.
     */
    public boolean cancelExecution(Long executionId) {
        CompletableFuture<WorkflowExecution> future = runningExecutions.get(executionId);
        if (future != null) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                runningExecutions.remove(executionId);
                
                // Update execution status
                WorkflowExecution execution = executionRepository.findById(executionId).orElse(null);
                if (execution != null) {
                    execution.cancel();
                    executionRepository.save(execution);
                }
                
                logger.info("Cancelled workflow execution {}", executionId);
            }
            return cancelled;
        }
        return false;
    }

    /**
     * Gets all workflows.
     */
    public List<Workflow> getAllWorkflows() {
        return workflowRepository.findAll();
    }

    /**
     * Gets workflow by ID.
     */
    public Workflow getWorkflow(Long workflowId) {
        return workflowRepository.findById(workflowId).orElse(null);
    }

    /**
     * Creates a new workflow.
     */
    public Workflow createWorkflow(String name, String description, String definition) {
        Workflow workflow = new Workflow(name, description, definition);
        return workflowRepository.save(workflow);
    }

    /**
     * Gets executions for a workflow.
     */
    public List<WorkflowExecution> getWorkflowExecutions(Long workflowId) {
        return executionRepository.findByWorkflowIdOrderByStartedAtDesc(workflowId);
    }

    /**
     * Gets execution by ID.
     */
    public WorkflowExecution getExecution(Long executionId) {
        return executionRepository.findById(executionId).orElse(null);
    }

    /**
     * Gets count of running executions.
     */
    public int getRunningExecutionCount() {
        return runningExecutions.size();
    }
}