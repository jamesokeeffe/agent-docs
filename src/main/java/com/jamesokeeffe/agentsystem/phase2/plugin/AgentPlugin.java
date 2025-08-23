package com.jamesokeeffe.agentsystem.phase2.plugin;

import java.util.Map;

/**
 * Interface that all agent plugins must implement.
 * 
 * Provides a standard contract for plugin functionality:
 * - Initialization and configuration
 * - Command execution
 * - Lifecycle management
 * - Metadata access
 * 
 * @author James O'Keeffe
 * @version 2.0.0
 * @since 2.0.0
 */
public interface AgentPlugin {

    /**
     * Initializes the plugin with configuration parameters.
     * 
     * @param configuration plugin-specific configuration map
     * @throws PluginException if initialization fails
     */
    void initialize(Map<String, Object> configuration) throws PluginException;

    /**
     * Executes the plugin with the given context.
     * 
     * @param context execution context containing input data and metadata
     * @return execution result with output data and status
     * @throws PluginException if execution fails
     */
    PluginResult execute(PluginContext context) throws PluginException;

    /**
     * Performs cleanup when the plugin is being unloaded.
     */
    void destroy();

    /**
     * Returns the plugin's unique name.
     * 
     * @return plugin name
     */
    String getName();

    /**
     * Returns the plugin's version.
     * 
     * @return plugin version
     */
    String getVersion();

    /**
     * Returns the plugin's type.
     * 
     * @return plugin type
     */
    PluginType getType();

    /**
     * Returns the plugin's description.
     * 
     * @return plugin description
     */
    String getDescription();

    /**
     * Checks if the plugin supports the given command.
     * 
     * @param command command to check
     * @return true if plugin can handle this command
     */
    boolean supports(String command);

    /**
     * Returns the plugin's current configuration.
     * 
     * @return configuration map
     */
    Map<String, Object> getConfiguration();

    /**
     * Validates the plugin's configuration.
     * 
     * @param configuration configuration to validate
     * @return true if configuration is valid
     */
    boolean validateConfiguration(Map<String, Object> configuration);
}