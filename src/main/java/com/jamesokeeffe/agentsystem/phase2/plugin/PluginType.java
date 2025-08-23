package com.jamesokeeffe.agentsystem.phase2.plugin;

/**
 * Enumeration of plugin types supported by the agent system.
 * 
 * Different types of plugins provide different capabilities:
 * - COMMAND_PROCESSOR: Custom command handling logic
 * - AI_INTEGRATION: External AI service integration
 * - DATA_TRANSFORMER: Data processing and transformation
 * - API_CONNECTOR: External API integration
 * - WORKFLOW_STEP: Custom workflow step implementation
 * 
 * @author James O'Keeffe
 * @version 2.0.0
 * @since 2.0.0
 */
public enum PluginType {
    
    /**
     * Plugin that processes custom commands.
     */
    COMMAND_PROCESSOR("Command Processor"),
    
    /**
     * Plugin that integrates with AI services.
     */
    AI_INTEGRATION("AI Integration"),
    
    /**
     * Plugin that transforms data between formats.
     */
    DATA_TRANSFORMER("Data Transformer"),
    
    /**
     * Plugin that connects to external APIs.
     */
    API_CONNECTOR("API Connector"),
    
    /**
     * Plugin that implements custom workflow steps.
     */
    WORKFLOW_STEP("Workflow Step"),
    
    /**
     * Plugin that provides custom analytics.
     */
    ANALYTICS("Analytics"),
    
    /**
     * Plugin that handles authentication.
     */
    AUTHENTICATION("Authentication"),
    
    /**
     * Plugin that provides monitoring capabilities.
     */
    MONITORING("Monitoring");

    private final String displayName;

    PluginType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}