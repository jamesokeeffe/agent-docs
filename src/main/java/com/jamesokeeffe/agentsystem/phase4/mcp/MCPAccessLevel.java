package com.jamesokeeffe.agentsystem.phase4.mcp;

/**
 * Enumeration of MCP resource access levels.
 * 
 * Defines the access permissions for MCP resources:
 * - READ: Read-only access
 * - WRITE: Write-only access
 * - READ_WRITE: Full read and write access
 * - EXECUTE: Execute permission for executable resources
 * - ADMIN: Full administrative access
 * 
 * @author James O'Keeffe
 * @version 4.0.0
 * @since 4.0.0
 */
public enum MCPAccessLevel {
    
    /**
     * Read-only access to the resource.
     */
    READ("Read"),
    
    /**
     * Write-only access to the resource.
     */
    WRITE("Write"),
    
    /**
     * Full read and write access to the resource.
     */
    READ_WRITE("Read/Write"),
    
    /**
     * Execute permission for executable resources.
     */
    EXECUTE("Execute"),
    
    /**
     * Full administrative access to the resource.
     */
    ADMIN("Admin");

    private final String displayName;

    MCPAccessLevel(String displayName) {
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