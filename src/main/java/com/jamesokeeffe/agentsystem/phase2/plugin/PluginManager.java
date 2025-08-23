package com.jamesokeeffe.agentsystem.phase2.plugin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing plugin lifecycle and execution.
 * 
 * Responsibilities:
 * - Plugin registration and loading
 * - Plugin execution coordination
 * - Configuration management
 * - Error handling and recovery
 * 
 * @author James O'Keeffe
 * @version 2.0.0
 * @since 2.0.0
 */
@Service
public class PluginManager {

    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);

    private final PluginRepository pluginRepository;
    private final ObjectMapper objectMapper;
    
    // Cache of loaded plugin instances
    private final Map<Long, AgentPlugin> loadedPlugins = new ConcurrentHashMap<>();
    
    // Registry of available plugin classes
    private final Map<String, Class<? extends AgentPlugin>> pluginRegistry = new HashMap<>();

    @Autowired
    public PluginManager(PluginRepository pluginRepository, ObjectMapper objectMapper) {
        this.pluginRepository = pluginRepository;
        this.objectMapper = objectMapper;
        registerBuiltInPlugins();
    }

    /**
     * Registers built-in plugins that come with the system.
     */
    private void registerBuiltInPlugins() {
        // Register built-in plugins here
        pluginRegistry.put("echo-plugin", EchoPlugin.class);
        pluginRegistry.put("time-plugin", TimePlugin.class);
        pluginRegistry.put("calculator-plugin", CalculatorPlugin.class);
        
        logger.info("Registered {} built-in plugins", pluginRegistry.size());
    }

    /**
     * Loads all active plugins for an agent.
     */
    public void loadPluginsForAgent(Long agentId) {
        try {
            List<Plugin> plugins = pluginRepository.findByAgentIdAndEnabledTrue(agentId);
            
            for (Plugin plugin : plugins) {
                try {
                    loadPlugin(plugin);
                } catch (PluginException e) {
                    logger.error("Failed to load plugin {} for agent {}: {}", 
                        plugin.getName(), agentId, e.getMessage());
                    plugin.markFailed();
                    pluginRepository.save(plugin);
                }
            }
            
            logger.info("Loaded {} plugins for agent {}", plugins.size(), agentId);
        } catch (Exception e) {
            logger.error("Error loading plugins for agent {}: {}", agentId, e.getMessage());
        }
    }

    /**
     * Loads a specific plugin instance.
     */
    private void loadPlugin(Plugin plugin) throws PluginException {
        try {
            // Get plugin class
            Class<? extends AgentPlugin> pluginClass = getPluginClass(plugin);
            
            // Create instance
            AgentPlugin pluginInstance = pluginClass.getDeclaredConstructor().newInstance();
            
            // Parse configuration
            Map<String, Object> config = parseConfiguration(plugin.getConfiguration());
            
            // Initialize plugin
            pluginInstance.initialize(config);
            
            // Cache the loaded instance
            loadedPlugins.put(plugin.getId(), pluginInstance);
            
            // Update plugin status
            plugin.activate();
            pluginRepository.save(plugin);
            
            logger.debug("Successfully loaded plugin: {}", plugin.getName());
        } catch (Exception e) {
            throw new PluginException(plugin.getName(), 
                PluginException.PluginErrorCode.INITIALIZATION_FAILED,
                "Failed to load plugin: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the plugin class for a plugin entity.
     */
    private Class<? extends AgentPlugin> getPluginClass(Plugin plugin) throws PluginException {
        // First try by class name
        if (plugin.getClassName() != null) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends AgentPlugin> clazz = 
                    (Class<? extends AgentPlugin>) Class.forName(plugin.getClassName());
                return clazz;
            } catch (ClassNotFoundException e) {
                logger.warn("Plugin class not found: {}", plugin.getClassName());
            }
        }
        
        // Fall back to registry lookup
        Class<? extends AgentPlugin> pluginClass = pluginRegistry.get(plugin.getName());
        if (pluginClass == null) {
            throw new PluginException(plugin.getName(),
                PluginException.PluginErrorCode.RESOURCE_NOT_FOUND,
                "Plugin class not found: " + plugin.getName());
        }
        
        return pluginClass;
    }

    /**
     * Parses plugin configuration from JSON string.
     */
    private Map<String, Object> parseConfiguration(String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            logger.warn("Failed to parse plugin configuration, using empty config: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Executes plugins for a given command on an agent.
     */
    public List<PluginResult> executePlugins(Long agentId, String command, Map<String, Object> parameters) {
        List<PluginResult> results = new ArrayList<>();
        
        try {
            List<Plugin> plugins = pluginRepository.findByAgentIdAndEnabledTrueOrderByLoadPriorityDesc(agentId);
            
            for (Plugin plugin : plugins) {
                AgentPlugin pluginInstance = loadedPlugins.get(plugin.getId());
                
                if (pluginInstance != null && pluginInstance.supports(command)) {
                    try {
                        PluginContext context = new PluginContext(command, parameters, agentId, null);
                        PluginResult result = pluginInstance.execute(context);
                        results.add(result);
                        
                        logger.debug("Plugin {} executed successfully for command: {}", 
                            plugin.getName(), command);
                    } catch (PluginException e) {
                        PluginResult errorResult = PluginResult.failure(
                            "Plugin execution failed: " + e.getMessage(), e);
                        results.add(errorResult);
                        
                        logger.error("Plugin {} failed to execute command {}: {}", 
                            plugin.getName(), command, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error executing plugins for agent {} and command {}: {}", 
                agentId, command, e.getMessage());
            results.add(PluginResult.failure("Plugin execution error: " + e.getMessage(), e));
        }
        
        return results;
    }

    /**
     * Unloads a plugin and removes it from cache.
     */
    public void unloadPlugin(Long pluginId) {
        AgentPlugin plugin = loadedPlugins.remove(pluginId);
        if (plugin != null) {
            try {
                plugin.destroy();
                logger.debug("Plugin {} unloaded successfully", plugin.getName());
            } catch (Exception e) {
                logger.warn("Error during plugin destruction: {}", e.getMessage());
            }
        }
    }

    /**
     * Gets all plugins for an agent.
     */
    public List<Plugin> getPluginsForAgent(Long agentId) {
        return pluginRepository.findByAgentId(agentId);
    }

    /**
     * Creates a new plugin registration.
     */
    public Plugin createPlugin(Long agentId, String name, PluginType type, String configuration) {
        Plugin plugin = new Plugin(agentId, name, type);
        plugin.setConfiguration(configuration);
        plugin.setStatus(PluginStatus.INSTALLED);
        
        return pluginRepository.save(plugin);
    }

    /**
     * Enables a plugin.
     */
    public void enablePlugin(Long pluginId) throws PluginException {
        Plugin plugin = pluginRepository.findById(pluginId)
            .orElseThrow(() -> new PluginException("Plugin not found: " + pluginId));
        
        plugin.enable();
        pluginRepository.save(plugin);
        
        // Load the plugin if not already loaded
        if (!loadedPlugins.containsKey(pluginId)) {
            loadPlugin(plugin);
        }
    }

    /**
     * Disables a plugin.
     */
    public void disablePlugin(Long pluginId) {
        Plugin plugin = pluginRepository.findById(pluginId).orElse(null);
        if (plugin != null) {
            plugin.disable();
            pluginRepository.save(plugin);
            unloadPlugin(pluginId);
        }
    }

    /**
     * Gets the count of loaded plugins.
     */
    public int getLoadedPluginCount() {
        return loadedPlugins.size();
    }

    /**
     * Gets available plugin types from registry.
     */
    public Set<String> getAvailablePluginTypes() {
        return new HashSet<>(pluginRegistry.keySet());
    }
}