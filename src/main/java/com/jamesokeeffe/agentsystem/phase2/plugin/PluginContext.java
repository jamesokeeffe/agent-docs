package com.jamesokeeffe.agentsystem.phase2.plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Context object passed to plugins during execution.
 * 
 * Contains:
 * - Input data and parameters
 * - Agent information
 * - Execution metadata
 * - Shared state between plugins
 * 
 * @author James O'Keeffe
 * @version 2.0.0
 * @since 2.0.0
 */
public class PluginContext {

    private final String command;
    private final Map<String, Object> parameters;
    private final Long agentId;
    private final String agentName;
    private final Map<String, Object> metadata;
    private final Map<String, Object> sharedState;

    public PluginContext(String command, Map<String, Object> parameters, Long agentId, String agentName) {
        this.command = command;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
        this.agentId = agentId;
        this.agentName = agentName;
        this.metadata = new HashMap<>();
        this.sharedState = new HashMap<>();
    }

    // Getters
    public String getCommand() {
        return command;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Long getAgentId() {
        return agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Map<String, Object> getSharedState() {
        return sharedState;
    }

    // Utility methods
    public Object getParameter(String key) {
        return parameters.get(key);
    }

    public <T> T getParameter(String key, Class<T> type) {
        Object value = parameters.get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    public void setParameter(String key, Object value) {
        parameters.put(key, value);
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getSharedData(String key) {
        return sharedState.get(key);
    }

    public void setSharedData(String key, Object value) {
        sharedState.put(key, value);
    }

    @Override
    public String toString() {
        return "PluginContext{" +
                "command='" + command + '\'' +
                ", agentId=" + agentId +
                ", agentName='" + agentName + '\'' +
                ", parametersCount=" + parameters.size() +
                '}';
    }
}