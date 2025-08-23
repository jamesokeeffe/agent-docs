package com.jamesokeeffe.agentsystem.model;

/**
 * Enumeration of agent types in the multi-agent system.
 * 
 * Each agent type represents a different specialization and capability set:
 * 
 * - GENERAL: General-purpose agent capable of handling various tasks
 * - CODE_REVIEWER: Specialized in code analysis, review, and quality assessment
 * - DOCUMENTATION: Focused on generating and maintaining documentation
 * - TESTING: Specialized in test creation, execution, and validation
 * - ORCHESTRATOR: Coordinates workflows and manages other agents
 * - MONITOR: Observes system health and performance metrics
 * 
 * @author James O'Keeffe
 * @version 1.0.0
 * @since 1.0.0
 */
public enum AgentType {
    
    /**
     * General-purpose agent for various tasks.
     * Default type for basic agent functionality.
     */
    GENERAL("General Purpose Agent", "Handles various general tasks"),
    
    /**
     * Code review specialist agent.
     * Introduced in Phase 2 for enhanced capabilities.
     */
    CODE_REVIEWER("Code Reviewer", "Analyzes and reviews code quality"),
    
    /**
     * Documentation generation agent.
     * Introduced in Phase 2 for enhanced capabilities.
     */
    DOCUMENTATION("Documentation Generator", "Creates and maintains documentation"),
    
    /**
     * Testing specialist agent.
     * Introduced in Phase 2 for enhanced capabilities.
     */
    TESTING("Test Creator", "Generates and executes tests"),
    
    /**
     * Workflow orchestration agent.
     * Introduced in Phase 3 for multi-agent coordination.
     */
    ORCHESTRATOR("Workflow Orchestrator", "Coordinates multi-agent workflows"),
    
    /**
     * System monitoring agent.
     * Introduced in Phase 3 for system observability.
     */
    MONITOR("System Monitor", "Monitors system health and performance");

    private final String displayName;
    private final String description;

    AgentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets the human-readable display name for this agent type.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the description of this agent type's purpose.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this agent type is available in the current phase.
     * Phase 1 supports only GENERAL agents.
     * 
     * @param phase the current system phase (1-4)
     * @return true if available in the specified phase
     */
    public boolean isAvailableInPhase(int phase) {
        return switch (this) {
            case GENERAL -> phase >= 1;
            case CODE_REVIEWER, DOCUMENTATION, TESTING -> phase >= 2;
            case ORCHESTRATOR, MONITOR -> phase >= 3;
        };
    }

    /**
     * Gets all agent types available in the specified phase.
     * 
     * @param phase the system phase (1-4)
     * @return array of available agent types
     */
    public static AgentType[] getAvailableInPhase(int phase) {
        return switch (phase) {
            case 1 -> new AgentType[]{GENERAL};
            case 2 -> new AgentType[]{GENERAL, CODE_REVIEWER, DOCUMENTATION, TESTING};
            case 3, 4 -> AgentType.values();
            default -> new AgentType[]{};
        };
    }
}