package com.jamesokeeffe.agentsystem.phase4.mcp;

/**
 * Enumeration of MCP tool types.
 * 
 * Different types of tools available via Model Context Protocol:
 * - FUNCTION: Pure function calls
 * - API: External API integrations
 * - DATABASE: Database query tools
 * - FILE_SYSTEM: File system operations
 * - COMPUTATION: Mathematical and computational tools
 * - COMMUNICATION: Communication and messaging tools
 * 
 * @author James O'Keeffe
 * @version 4.0.0
 * @since 4.0.0
 */
public enum MCPToolType {
    
    /**
     * Pure function call tools.
     */
    FUNCTION("Function"),
    
    /**
     * External API integration tools.
     */
    API("API"),
    
    /**
     * Database query and manipulation tools.
     */
    DATABASE("Database"),
    
    /**
     * File system operation tools.
     */
    FILE_SYSTEM("File System"),
    
    /**
     * Mathematical and computational tools.
     */
    COMPUTATION("Computation"),
    
    /**
     * Communication and messaging tools.
     */
    COMMUNICATION("Communication"),
    
    /**
     * Data processing and transformation tools.
     */
    DATA_PROCESSING("Data Processing"),
    
    /**
     * Search and retrieval tools.
     */
    SEARCH("Search"),
    
    /**
     * Monitoring and observability tools.
     */
    MONITORING("Monitoring"),
    
    /**
     * Security and authentication tools.
     */
    SECURITY("Security");

    private final String displayName;

    MCPToolType(String displayName) {
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