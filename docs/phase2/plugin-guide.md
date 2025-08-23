# Phase 2 Plugin System Guide

## Overview

This guide covers the implementation of a dynamic plugin system that allows agents to extend their capabilities through hot-loadable plugins.

## Plugin Architecture

### 1. Core Plugin Interface

```java
public interface AgentPlugin {
    /**
     * Returns the unique name of this plugin
     */
    String getName();
    
    /**
     * Returns the version of this plugin
     */
    String getVersion();
    
    /**
     * Returns a description of what this plugin does
     */
    String getDescription();
    
    /**
     * Initialize the plugin with given context
     */
    void initialize(PluginContext context) throws PluginException;
    
    /**
     * Execute the plugin with given request
     */
    PluginResult execute(PluginRequest request) throws PluginException;
    
    /**
     * Check if plugin is healthy and ready to execute
     */
    boolean isHealthy();
    
    /**
     * Shutdown the plugin and clean up resources
     */
    void shutdown();
    
    /**
     * Get plugin configuration schema
     */
    default PluginConfigSchema getConfigSchema() {
        return PluginConfigSchema.empty();
    }
}
```

### 2. Plugin Context

```java
public class PluginContext {
    private final String agentId;
    private final Map<String, Object> configuration;
    private final PluginLogger logger;
    private final PluginMetrics metrics;
    private final PluginResourceManager resourceManager;
    
    public PluginContext(String agentId, Map<String, Object> configuration) {
        this.agentId = agentId;
        this.configuration = configuration;
        this.logger = new PluginLogger(agentId);
        this.metrics = new PluginMetrics(agentId);
        this.resourceManager = new PluginResourceManager();
    }
    
    public String getAgentId() { return agentId; }
    public Map<String, Object> getConfiguration() { return configuration; }
    public PluginLogger getLogger() { return logger; }
    public PluginMetrics getMetrics() { return metrics; }
    public PluginResourceManager getResourceManager() { return resourceManager; }
    
    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(String key, Class<T> type, T defaultValue) {
        Object value = configuration.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }
}
```

### 3. Plugin Manager

```java
@Service
@Slf4j
public class PluginManager {
    
    private final Map<String, AgentPlugin> loadedPlugins = new ConcurrentHashMap<>();
    private final Map<String, PluginContext> pluginContexts = new ConcurrentHashMap<>();
    private final PluginConfigService configService;
    private final PluginExecutionService executionService;
    
    @Autowired
    public PluginManager(PluginConfigService configService, 
                        PluginExecutionService executionService) {
        this.configService = configService;
        this.executionService = executionService;
    }
    
    @PostConstruct
    public void initializePlugins() {
        loadBuiltInPlugins();
        loadExternalPlugins();
    }
    
    public void loadPlugin(String pluginName, String pluginClass, Long agentId) 
            throws PluginException {
        try {
            Class<?> clazz = Class.forName(pluginClass);
            AgentPlugin plugin = (AgentPlugin) clazz.getDeclaredConstructor().newInstance();
            
            Map<String, Object> config = configService.getPluginConfig(pluginName, agentId);
            PluginContext context = new PluginContext(agentId.toString(), config);
            
            plugin.initialize(context);
            
            String pluginKey = getPluginKey(pluginName, agentId);
            loadedPlugins.put(pluginKey, plugin);
            pluginContexts.put(pluginKey, context);
            
            log.info("Successfully loaded plugin: {} for agent: {}", pluginName, agentId);
            
        } catch (Exception e) {
            throw new PluginException("Failed to load plugin: " + pluginName, e);
        }
    }
    
    public void unloadPlugin(String pluginName, Long agentId) {
        String pluginKey = getPluginKey(pluginName, agentId);
        AgentPlugin plugin = loadedPlugins.remove(pluginKey);
        
        if (plugin != null) {
            try {
                plugin.shutdown();
                pluginContexts.remove(pluginKey);
                log.info("Successfully unloaded plugin: {} for agent: {}", pluginName, agentId);
            } catch (Exception e) {
                log.error("Error during plugin shutdown: {}", pluginName, e);
            }
        }
    }
    
    public PluginResult executePlugin(String pluginName, Long agentId, 
                                    PluginRequest request) throws PluginException {
        String pluginKey = getPluginKey(pluginName, agentId);
        AgentPlugin plugin = loadedPlugins.get(pluginKey);
        
        if (plugin == null) {
            throw new PluginException("Plugin not found: " + pluginName);
        }
        
        if (!plugin.isHealthy()) {
            throw new PluginException("Plugin not healthy: " + pluginName);
        }
        
        return executionService.executeWithTimeout(plugin, request, Duration.ofSeconds(30));
    }
    
    public List<PluginInfo> getLoadedPlugins(Long agentId) {
        return loadedPlugins.entrySet().stream()
            .filter(entry -> entry.getKey().endsWith(":" + agentId))
            .map(entry -> {
                AgentPlugin plugin = entry.getValue();
                return PluginInfo.builder()
                    .name(plugin.getName())
                    .version(plugin.getVersion())
                    .description(plugin.getDescription())
                    .healthy(plugin.isHealthy())
                    .agentId(agentId)
                    .build();
            })
            .collect(Collectors.toList());
    }
    
    private String getPluginKey(String pluginName, Long agentId) {
        return pluginName + ":" + agentId;
    }
    
    private void loadBuiltInPlugins() {
        // Load built-in plugins available to all agents
        registerBuiltInPlugin("echo", EchoPlugin.class);
        registerBuiltInPlugin("time", TimePlugin.class);
        registerBuiltInPlugin("calculator", CalculatorPlugin.class);
    }
    
    private void registerBuiltInPlugin(String name, Class<? extends AgentPlugin> pluginClass) {
        try {
            AgentPlugin plugin = pluginClass.getDeclaredConstructor().newInstance();
            PluginContext context = new PluginContext("system", Map.of());
            plugin.initialize(context);
            
            loadedPlugins.put(name + ":system", plugin);
            pluginContexts.put(name + ":system", context);
            
            log.info("Registered built-in plugin: {}", name);
        } catch (Exception e) {
            log.error("Failed to register built-in plugin: {}", name, e);
        }
    }
}
```

## Built-in Plugins

### 1. Echo Plugin

```java
@Component
public class EchoPlugin implements AgentPlugin {
    
    private static final String NAME = "echo";
    private static final String VERSION = "1.0.0";
    private static final String DESCRIPTION = "Simple echo plugin that returns the input message";
    
    private PluginContext context;
    private boolean initialized = false;
    
    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public String getVersion() {
        return VERSION;
    }
    
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
    
    @Override
    public void initialize(PluginContext context) throws PluginException {
        this.context = context;
        this.initialized = true;
        context.getLogger().info("Echo plugin initialized for agent: {}", context.getAgentId());
    }
    
    @Override
    public PluginResult execute(PluginRequest request) throws PluginException {
        if (!initialized) {
            throw new PluginException("Plugin not initialized");
        }
        
        context.getMetrics().incrementExecutionCount();
        long startTime = System.currentTimeMillis();
        
        try {
            String message = request.getParameter("message", String.class);
            if (message == null) {
                throw new PluginException("Missing required parameter: message");
            }
            
            String prefix = context.getConfigValue("prefix", String.class, "Echo: ");
            String response = prefix + message;
            
            PluginResult result = PluginResult.success(Map.of("response", response));
            
            context.getMetrics().recordExecutionTime(System.currentTimeMillis() - startTime);
            context.getLogger().debug("Echo plugin executed successfully");
            
            return result;
            
        } catch (Exception e) {
            context.getMetrics().incrementErrorCount();
            throw new PluginException("Echo plugin execution failed", e);
        }
    }
    
    @Override
    public boolean isHealthy() {
        return initialized;
    }
    
    @Override
    public void shutdown() {
        initialized = false;
        if (context != null) {
            context.getLogger().info("Echo plugin shutdown for agent: {}", context.getAgentId());
        }
    }
    
    @Override
    public PluginConfigSchema getConfigSchema() {
        return PluginConfigSchema.builder()
            .addProperty("prefix", String.class, "Echo: ", "Prefix to add to echoed messages")
            .build();
    }
}
```

### 2. Time Plugin

```java
@Component
public class TimePlugin implements AgentPlugin {
    
    private static final String NAME = "time";
    private static final String VERSION = "1.0.0";
    private static final String DESCRIPTION = "Provides current time and date information";
    
    private PluginContext context;
    private boolean initialized = false;
    
    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public String getVersion() {
        return VERSION;
    }
    
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
    
    @Override
    public void initialize(PluginContext context) throws PluginException {
        this.context = context;
        this.initialized = true;
        context.getLogger().info("Time plugin initialized for agent: {}", context.getAgentId());
    }
    
    @Override
    public PluginResult execute(PluginRequest request) throws PluginException {
        if (!initialized) {
            throw new PluginException("Plugin not initialized");
        }
        
        context.getMetrics().incrementExecutionCount();
        long startTime = System.currentTimeMillis();
        
        try {
            String format = request.getParameter("format", String.class);
            String timezone = request.getParameter("timezone", String.class);
            
            ZoneId zoneId = timezone != null ? ZoneId.of(timezone) : ZoneId.systemDefault();
            LocalDateTime now = LocalDateTime.now(zoneId);
            
            Map<String, Object> result = new HashMap<>();
            
            if (format != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                result.put("formatted", now.format(formatter));
            } else {
                result.put("iso", now.toString());
                result.put("epoch", now.atZone(zoneId).toEpochSecond());
                result.put("timezone", zoneId.toString());
            }
            
            context.getMetrics().recordExecutionTime(System.currentTimeMillis() - startTime);
            return PluginResult.success(result);
            
        } catch (Exception e) {
            context.getMetrics().incrementErrorCount();
            throw new PluginException("Time plugin execution failed", e);
        }
    }
    
    @Override
    public boolean isHealthy() {
        return initialized;
    }
    
    @Override
    public void shutdown() {
        initialized = false;
        if (context != null) {
            context.getLogger().info("Time plugin shutdown for agent: {}", context.getAgentId());
        }
    }
    
    @Override
    public PluginConfigSchema getConfigSchema() {
        return PluginConfigSchema.builder()
            .addProperty("defaultTimezone", String.class, "UTC", "Default timezone for time operations")
            .addProperty("defaultFormat", String.class, "yyyy-MM-dd HH:mm:ss", "Default date format")
            .build();
    }
}
```

### 3. Calculator Plugin

```java
@Component
public class CalculatorPlugin implements AgentPlugin {
    
    private static final String NAME = "calculator";
    private static final String VERSION = "1.0.0";
    private static final String DESCRIPTION = "Performs mathematical calculations";
    
    private PluginContext context;
    private boolean initialized = false;
    
    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public String getVersion() {
        return VERSION;
    }
    
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
    
    @Override
    public void initialize(PluginContext context) throws PluginException {
        this.context = context;
        this.initialized = true;
        context.getLogger().info("Calculator plugin initialized for agent: {}", context.getAgentId());
    }
    
    @Override
    public PluginResult execute(PluginRequest request) throws PluginException {
        if (!initialized) {
            throw new PluginException("Plugin not initialized");
        }
        
        context.getMetrics().incrementExecutionCount();
        long startTime = System.currentTimeMillis();
        
        try {
            String operation = request.getParameter("operation", String.class);
            if (operation == null) {
                throw new PluginException("Missing required parameter: operation");
            }
            
            double result = performCalculation(operation);
            
            Map<String, Object> response = Map.of(
                "operation", operation,
                "result", result
            );
            
            context.getMetrics().recordExecutionTime(System.currentTimeMillis() - startTime);
            return PluginResult.success(response);
            
        } catch (Exception e) {
            context.getMetrics().incrementErrorCount();
            throw new PluginException("Calculator plugin execution failed", e);
        }
    }
    
    private double performCalculation(String operation) throws PluginException {
        try {
            // Simple expression evaluator (for demo purposes)
            // In production, use a proper expression parser like JEXL
            if (operation.contains("+")) {
                String[] parts = operation.split("\\+");
                return Double.parseDouble(parts[0].trim()) + Double.parseDouble(parts[1].trim());
            } else if (operation.contains("-")) {
                String[] parts = operation.split("-");
                return Double.parseDouble(parts[0].trim()) - Double.parseDouble(parts[1].trim());
            } else if (operation.contains("*")) {
                String[] parts = operation.split("\\*");
                return Double.parseDouble(parts[0].trim()) * Double.parseDouble(parts[1].trim());
            } else if (operation.contains("/")) {
                String[] parts = operation.split("/");
                double divisor = Double.parseDouble(parts[1].trim());
                if (divisor == 0) {
                    throw new PluginException("Division by zero");
                }
                return Double.parseDouble(parts[0].trim()) / divisor;
            } else {
                throw new PluginException("Unsupported operation: " + operation);
            }
        } catch (NumberFormatException e) {
            throw new PluginException("Invalid number format in operation: " + operation);
        }
    }
    
    @Override
    public boolean isHealthy() {
        return initialized;
    }
    
    @Override
    public void shutdown() {
        initialized = false;
        if (context != null) {
            context.getLogger().info("Calculator plugin shutdown for agent: {}", context.getAgentId());
        }
    }
    
    @Override
    public PluginConfigSchema getConfigSchema() {
        return PluginConfigSchema.builder()
            .addProperty("precision", Integer.class, 2, "Number of decimal places in results")
            .addProperty("allowComplexOperations", Boolean.class, false, "Enable complex mathematical operations")
            .build();
    }
}
```

## Plugin Configuration

### 1. Configuration Service

```java
@Service
public class PluginConfigService {
    
    @Autowired
    private PluginConfigRepository configRepository;
    
    public Map<String, Object> getPluginConfig(String pluginName, Long agentId) {
        Optional<PluginConfig> config = configRepository.findByPluginNameAndAgentId(pluginName, agentId);
        
        if (config.isPresent()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(config.get().getConfigJson(), Map.class);
            } catch (Exception e) {
                log.error("Failed to parse plugin config for {}: {}", pluginName, e.getMessage());
                return new HashMap<>();
            }
        }
        
        return new HashMap<>();
    }
    
    public void updatePluginConfig(String pluginName, Long agentId, Map<String, Object> config) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String configJson = mapper.writeValueAsString(config);
            
            PluginConfig pluginConfig = configRepository
                .findByPluginNameAndAgentId(pluginName, agentId)
                .orElse(new PluginConfig());
            
            pluginConfig.setPluginName(pluginName);
            pluginConfig.setAgentId(agentId);
            pluginConfig.setConfigJson(configJson);
            pluginConfig.setUpdatedAt(LocalDateTime.now());
            
            configRepository.save(pluginConfig);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to update plugin config", e);
        }
    }
    
    public void enablePlugin(String pluginName, Long agentId) {
        PluginConfig config = configRepository
            .findByPluginNameAndAgentId(pluginName, agentId)
            .orElse(new PluginConfig());
        
        config.setPluginName(pluginName);
        config.setAgentId(agentId);
        config.setEnabled(true);
        config.setUpdatedAt(LocalDateTime.now());
        
        configRepository.save(config);
    }
    
    public void disablePlugin(String pluginName, Long agentId) {
        configRepository.findByPluginNameAndAgentId(pluginName, agentId)
            .ifPresent(config -> {
                config.setEnabled(false);
                config.setUpdatedAt(LocalDateTime.now());
                configRepository.save(config);
            });
    }
}
```

## Plugin API Endpoints

### 1. Plugin Controller

```java
@RestController
@RequestMapping("/api/v1/plugins")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*")
public class PluginController {
    
    @Autowired
    private PluginManager pluginManager;
    
    @Autowired
    private PluginConfigService configService;
    
    @GetMapping("/agent/{agentId}")
    public ResponseEntity<List<PluginInfo>> getAgentPlugins(@PathVariable Long agentId) {
        List<PluginInfo> plugins = pluginManager.getLoadedPlugins(agentId);
        return ResponseEntity.ok(plugins);
    }
    
    @PostMapping("/{pluginId}/enable")
    public ResponseEntity<ApiResponse> enablePlugin(
            @PathVariable String pluginId,
            @RequestParam Long agentId) {
        try {
            configService.enablePlugin(pluginId, agentId);
            return ResponseEntity.ok(ApiResponse.success("Plugin enabled successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to enable plugin: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{pluginId}/disable")
    public ResponseEntity<ApiResponse> disablePlugin(
            @PathVariable String pluginId,
            @RequestParam Long agentId) {
        try {
            configService.disablePlugin(pluginId, agentId);
            pluginManager.unloadPlugin(pluginId, agentId);
            return ResponseEntity.ok(ApiResponse.success("Plugin disabled successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to disable plugin: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{pluginId}/test")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    public ResponseEntity<PluginTestResult> testPlugin(
            @PathVariable String pluginId,
            @RequestParam Long agentId,
            @RequestBody Map<String, Object> testData) {
        try {
            PluginRequest request = new PluginRequest(testData);
            PluginResult result = pluginManager.executePlugin(pluginId, agentId, request);
            
            return ResponseEntity.ok(PluginTestResult.builder()
                .success(result.isSuccess())
                .result(result.getData())
                .executionTime(result.getExecutionTime())
                .build());
                
        } catch (Exception e) {
            return ResponseEntity.ok(PluginTestResult.builder()
                .success(false)
                .error(e.getMessage())
                .build());
        }
    }
    
    @GetMapping("/{pluginId}/config")
    public ResponseEntity<Map<String, Object>> getPluginConfig(
            @PathVariable String pluginId,
            @RequestParam Long agentId) {
        Map<String, Object> config = configService.getPluginConfig(pluginId, agentId);
        return ResponseEntity.ok(config);
    }
    
    @PutMapping("/{pluginId}/config")
    public ResponseEntity<ApiResponse> updatePluginConfig(
            @PathVariable String pluginId,
            @RequestParam Long agentId,
            @RequestBody Map<String, Object> config) {
        try {
            configService.updatePluginConfig(pluginId, agentId, config);
            return ResponseEntity.ok(ApiResponse.success("Plugin configuration updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to update configuration: " + e.getMessage()));
        }
    }
}
```

## Plugin Testing

### 1. Plugin Test Framework

```java
@SpringBootTest
class PluginSystemTest {
    
    @Autowired
    private PluginManager pluginManager;
    
    @Test
    void shouldLoadAndExecuteEchoPlugin() throws PluginException {
        // Given
        Long agentId = 1L;
        Map<String, Object> requestData = Map.of("message", "Hello, World!");
        PluginRequest request = new PluginRequest(requestData);
        
        // When
        PluginResult result = pluginManager.executePlugin("echo", agentId, request);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().get("response")).isEqualTo("Echo: Hello, World!");
    }
    
    @Test
    void shouldHandlePluginConfigurationUpdates() {
        // Given
        Long agentId = 1L;
        Map<String, Object> config = Map.of("prefix", "Custom: ");
        
        // When
        configService.updatePluginConfig("echo", agentId, config);
        PluginRequest request = new PluginRequest(Map.of("message", "Test"));
        PluginResult result = pluginManager.executePlugin("echo", agentId, request);
        
        // Then
        assertThat(result.getData().get("response")).isEqualTo("Custom: Test");
    }
}
```

This plugin system provides a flexible, extensible foundation for adding capabilities to agents while maintaining security and performance.