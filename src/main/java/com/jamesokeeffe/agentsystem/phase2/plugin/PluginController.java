package com.jamesokeeffe.agentsystem.phase2.plugin;

import com.jamesokeeffe.agentsystem.controller.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for plugin management operations.
 * 
 * Provides endpoints for:
 * - Plugin CRUD operations
 * - Plugin activation/deactivation
 * - Plugin execution testing
 * - Plugin discovery and registry
 * 
 * @author James O'Keeffe
 * @version 2.0.0
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/v1/plugins")
@CrossOrigin(origins = "*")
public class PluginController {

    private static final Logger logger = LoggerFactory.getLogger(PluginController.class);

    private final PluginManager pluginManager;

    @Autowired
    public PluginController(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    /**
     * Get all plugins for an agent.
     */
    @GetMapping("/agent/{agentId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<Plugin>>> getPluginsByAgent(@PathVariable Long agentId) {
        try {
            List<Plugin> plugins = pluginManager.getPluginsForAgent(agentId);
            logger.debug("Retrieved {} plugins for agent {}", plugins.size(), agentId);
            return ResponseEntity.ok(ApiResponse.success(plugins));
        } catch (Exception e) {
            logger.error("Error retrieving plugins for agent {}: {}", agentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("PLUGIN_RETRIEVAL_ERROR", "Failed to retrieve plugins"));
        }
    }

    /**
     * Create a new plugin for an agent.
     */
    @PostMapping("/agent/{agentId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Plugin>> createPlugin(
            @PathVariable Long agentId,
            @Valid @RequestBody CreatePluginRequest request) {
        try {
            Plugin plugin = pluginManager.createPlugin(
                agentId, 
                request.getName(), 
                request.getType(), 
                request.getConfiguration()
            );
            
            logger.info("Created plugin {} for agent {}", plugin.getName(), agentId);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(plugin));
        } catch (Exception e) {
            logger.error("Error creating plugin for agent {}: {}", agentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("PLUGIN_CREATION_ERROR", "Failed to create plugin"));
        }
    }

    /**
     * Enable a plugin.
     */
    @PostMapping("/{pluginId}/enable")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> enablePlugin(@PathVariable Long pluginId) {
        try {
            pluginManager.enablePlugin(pluginId);
            logger.info("Enabled plugin {}", pluginId);
            return ResponseEntity.ok(ApiResponse.success("Plugin enabled successfully"));
        } catch (Exception e) {
            logger.error("Error enabling plugin {}: {}", pluginId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("PLUGIN_ENABLE_ERROR", "Failed to enable plugin"));
        }
    }

    /**
     * Disable a plugin.
     */
    @PostMapping("/{pluginId}/disable")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> disablePlugin(@PathVariable Long pluginId) {
        try {
            pluginManager.disablePlugin(pluginId);
            logger.info("Disabled plugin {}", pluginId);
            return ResponseEntity.ok(ApiResponse.success("Plugin disabled successfully"));
        } catch (Exception e) {
            logger.error("Error disabling plugin {}: {}", pluginId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("PLUGIN_DISABLE_ERROR", "Failed to disable plugin"));
        }
    }

    /**
     * Test plugin execution.
     */
    @PostMapping("/{pluginId}/test")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<PluginResult>>> testPlugin(
            @PathVariable Long pluginId,
            @RequestBody TestPluginRequest request) {
        try {
            // Note: This is a simplified test execution
            // In a real implementation, you'd need to validate the plugin ID belongs to the agent
            List<PluginResult> results = pluginManager.executePlugins(
                request.getAgentId(), 
                request.getCommand(), 
                request.getParameters()
            );
            
            logger.debug("Tested plugin {} with command: {}", pluginId, request.getCommand());
            return ResponseEntity.ok(ApiResponse.success(results));
        } catch (Exception e) {
            logger.error("Error testing plugin {}: {}", pluginId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("PLUGIN_TEST_ERROR", "Failed to test plugin"));
        }
    }

    /**
     * Get available plugin types.
     */
    @GetMapping("/types")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<PluginType[]>> getPluginTypes() {
        try {
            PluginType[] types = PluginType.values();
            return ResponseEntity.ok(ApiResponse.success(types));
        } catch (Exception e) {
            logger.error("Error retrieving plugin types: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("PLUGIN_TYPES_ERROR", "Failed to retrieve plugin types"));
        }
    }

    /**
     * Get system plugin statistics.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PluginStats>> getPluginStats() {
        try {
            PluginStats stats = new PluginStats(
                pluginManager.getLoadedPluginCount(),
                pluginManager.getAvailablePluginTypes().size()
            );
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            logger.error("Error retrieving plugin stats: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("PLUGIN_STATS_ERROR", "Failed to retrieve plugin statistics"));
        }
    }

    // DTOs
    public static class CreatePluginRequest {
        private String name;
        private PluginType type;
        private String configuration;
        private String description;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public PluginType getType() { return type; }
        public void setType(PluginType type) { this.type = type; }
        public String getConfiguration() { return configuration; }
        public void setConfiguration(String configuration) { this.configuration = configuration; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class TestPluginRequest {
        private Long agentId;
        private String command;
        private Map<String, Object> parameters;

        // Getters and setters
        public Long getAgentId() { return agentId; }
        public void setAgentId(Long agentId) { this.agentId = agentId; }
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }

    public static class PluginStats {
        private final int loadedPlugins;
        private final int availableTypes;

        public PluginStats(int loadedPlugins, int availableTypes) {
            this.loadedPlugins = loadedPlugins;
            this.availableTypes = availableTypes;
        }

        public int getLoadedPlugins() { return loadedPlugins; }
        public int getAvailableTypes() { return availableTypes; }
    }
}