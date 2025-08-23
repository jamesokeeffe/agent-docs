package com.jamesokeeffe.agentsystem.phase3.orchestration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Workflow step definition.
 * 
 * Represents a single step in a workflow:
 * - Step type and configuration
 * - Input/output parameters
 * - Dependencies and conditions
 * 
 * @author James O'Keeffe
 * @version 3.0.0
 * @since 3.0.0
 */
public class WorkflowStep {

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("description")
    private String description;

    @JsonProperty("parameters")
    private Map<String, Object> parameters;

    @JsonProperty("dependencies")
    private List<String> dependencies;

    @JsonProperty("condition")
    private String condition;

    @JsonProperty("timeout")
    private Integer timeoutSeconds;

    @JsonProperty("retry")
    private Integer retryCount;

    @JsonProperty("onError")
    private String onError;

    // Constructors
    public WorkflowStep() {}

    public WorkflowStep(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public WorkflowStep(String name, String type, Map<String, Object> parameters) {
        this(name, type);
        this.parameters = parameters;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
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

    public String getOnError() {
        return onError;
    }

    public void setOnError(String onError) {
        this.onError = onError;
    }

    @Override
    public String toString() {
        return "WorkflowStep{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", timeout=" + timeoutSeconds +
                ", dependencies=" + (dependencies != null ? dependencies.size() : 0) +
                '}';
    }
}