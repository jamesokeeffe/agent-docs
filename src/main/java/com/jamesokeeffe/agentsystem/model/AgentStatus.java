package com.jamesokeeffe.agentsystem.model;

/**
 * Enumeration of possible agent statuses in the multi-agent system.
 * 
 * Agent status indicates the current operational state:
 * 
 * - IDLE: Agent is available and ready to accept commands
 * - BUSY: Agent is currently executing a command or task
 * - OFFLINE: Agent is unavailable (maintenance, error, or shutdown)
 * - ERROR: Agent encountered an error and needs attention
 * 
 * @author James O'Keeffe
 * @version 1.0.0
 * @since 1.0.0
 */
public enum AgentStatus {
    
    /**
     * Agent is idle and ready to accept new commands.
     * This is the default state for available agents.
     */
    IDLE("Idle", "Ready to accept commands", true),
    
    /**
     * Agent is currently busy executing a command.
     * Agent cannot accept new commands in this state.
     */
    BUSY("Busy", "Currently executing a command", false),
    
    /**
     * Agent is offline and unavailable.
     * May be due to maintenance, shutdown, or configuration.
     */
    OFFLINE("Offline", "Agent is not available", false),
    
    /**
     * Agent is in an error state.
     * Requires intervention before it can resume normal operation.
     */
    ERROR("Error", "Agent encountered an error", false);

    private final String displayName;
    private final String description;
    private final boolean available;

    AgentStatus(String displayName, String description, boolean available) {
        this.displayName = displayName;
        this.description = description;
        this.available = available;
    }

    /**
     * Gets the human-readable display name for this status.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the description of what this status means.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if an agent with this status can accept new commands.
     * 
     * @return true if the agent is available for commands
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Gets all statuses that indicate an agent is operational.
     * 
     * @return array of operational statuses
     */
    public static AgentStatus[] getOperationalStatuses() {
        return new AgentStatus[]{IDLE, BUSY};
    }

    /**
     * Gets all statuses that indicate an agent is unavailable.
     * 
     * @return array of unavailable statuses
     */
    public static AgentStatus[] getUnavailableStatuses() {
        return new AgentStatus[]{OFFLINE, ERROR};
    }
}