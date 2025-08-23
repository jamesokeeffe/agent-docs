package com.jamesokeeffe.agentsystem.phase3.orchestration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Workflow definition data structure.
 * 
 * Represents the structure and flow of a workflow:
 * - Steps and their execution order
 * - Dependencies between steps
 * - Configuration and parameters
 * 
 * @author James O'Keeffe
 * @version 3.0.0
 * @since 3.0.0
 */
public class WorkflowDefinition {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("version")
    private String version;

    @JsonProperty("steps")
    private List<WorkflowStep> steps;

    @JsonProperty("variables")
    private Map<String, Object> variables;

    @JsonProperty("timeout")
    private Integer timeoutSeconds;

    @JsonProperty("retry")
    private Integer retryCount;

    // Constructors
    public WorkflowDefinition() {}

    public WorkflowDefinition(String name, List<WorkflowStep> steps) {
        this.name = name;
        this.steps = steps;
    }

    // Getters and setters
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<WorkflowStep> getSteps() {
        return steps;
    }

    public void setSteps(List<WorkflowStep> steps) {
        this.steps = steps;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    @Override
    public String toString() {
        return "WorkflowDefinition{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", stepsCount=" + (steps != null ? steps.size() : 0) +
                ", timeout=" + timeoutSeconds +
                '}';
    }
}