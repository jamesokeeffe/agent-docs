package com.jamesokeeffe.agentsystem.phase2.plugin;

/**
 * Enumeration of plugin status values.
 * 
 * Represents the current state of a plugin in the system:
 * - INSTALLED: Plugin is registered but not loaded
 * - ACTIVE: Plugin is loaded and available for execution
 * - INACTIVE: Plugin is loaded but disabled
 * - FAILED: Plugin failed to load or execute
 * - UNINSTALLED: Plugin has been removed from the system
 * 
 * @author James O'Keeffe
 * @version 2.0.0
 * @since 2.0.0
 */
public enum PluginStatus {
    
    /**
     * Plugin is registered in the system but not yet loaded.
     */
    INSTALLED("Installed"),
    
    /**
     * Plugin is loaded and ready for execution.
     */
    ACTIVE("Active"),
    
    /**
     * Plugin is loaded but temporarily disabled.
     */
    INACTIVE("Inactive"),
    
    /**
     * Plugin failed to load or encountered an error.
     */
    FAILED("Failed"),
    
    /**
     * Plugin has been removed from the system.
     */
    UNINSTALLED("Uninstalled");

    private final String displayName;

    PluginStatus(String displayName) {
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