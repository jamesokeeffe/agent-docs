# Phase 4 Architecture: Complete Production System

## System Architecture Overview

Phase 4 integrates all components into a production-ready, scalable multi-agent system.

```
┌─────────────────────────────────────────────────────────────┐
│                      Frontend Layer                         │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │React Web UI │ │VS Code Ext  │ │CLI Interface│          │
│  │Dashboard    │ │Language Srv │ │Admin Tools  │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
├─────────────────────────────────────────────────────────────┤
│                     API Gateway                             │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │Rate Limiter │ │Load Balancer│ │Request Router│          │
│  │Auth Filter  │ │Circuit Break│ │Response Cache│          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
├─────────────────────────────────────────────────────────────┤
│                 Application Services                        │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │Agent Manager│ │Workflow Eng │ │Plugin System│          │
│  │Security Core│ │Task Orchest │ │MCP Server   │          │
│  │Config Mgmt  │ │Event Bus    │ │Tool Registry│          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
├─────────────────────────────────────────────────────────────┤
│                LLM Integration Layer                        │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │Ollama Client│ │LocalAI Conn │ │OpenAI Adapt │          │
│  │Model Pool   │ │Context Mgmt │ │Response Proc│          │
│  │Load Balance │ │Token Mgmt   │ │Error Handle │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
├─────────────────────────────────────────────────────────────┤
│                 Data & Storage Layer                        │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │H2 Database  │ │Redis Cache  │ │File Storage │          │
│  │Agent Data   │ │Session Data │ │Code Repos   │          │
│  │Metrics      │ │Query Cache  │ │Artifacts    │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
├─────────────────────────────────────────────────────────────┤
│              Monitoring & Observability                    │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │Prometheus   │ │Grafana      │ │ELK Stack    │          │
│  │Metrics      │ │Dashboards   │ │Log Analysis │          │
│  │Alerts       │ │Visualization│ │Search       │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

## Local LLM Integration

### 1. Ollama Integration Service

```java
@Service
@Slf4j
public class OllamaIntegrationService implements LLMIntegrationService {
    
    private final OllamaClient ollamaClient;
    private final ModelManager modelManager;
    private final ContextManager contextManager;
    private final ResponseProcessor responseProcessor;
    
    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;
    
    @Value("${ollama.default-model:llama2}")
    private String defaultModel;
    
    @PostConstruct
    public void initialize() {
        ollamaClient = OllamaClient.builder()
            .baseUrl(ollamaBaseUrl)
            .timeout(Duration.ofMinutes(2))
            .build();
        
        // Verify connection and load available models
        loadAvailableModels();
        
        log.info("Ollama integration initialized with {} models", 
            modelManager.getAvailableModels().size());
    }
    
    @Override
    public LLMResponse generateCompletion(LLMRequest request) {
        String modelName = request.getModel() != null ? request.getModel() : defaultModel;
        
        try {
            // Validate model availability
            if (!modelManager.isModelAvailable(modelName)) {
                throw new ModelNotAvailableException("Model not available: " + modelName);
            }
            
            // Build context with conversation history
            String fullContext = contextManager.buildContext(request);
            
            // Create Ollama request
            OllamaGenerateRequest ollamaRequest = OllamaGenerateRequest.builder()
                .model(modelName)
                .prompt(fullContext)
                .temperature(request.getTemperature())
                .topP(request.getTopP())
                .maxTokens(request.getMaxTokens())
                .stop(request.getStopSequences())
                .build();
            
            // Execute request with timeout and retry
            OllamaGenerateResponse ollamaResponse = executeWithRetry(ollamaRequest);
            
            // Process and format response
            return responseProcessor.processOllamaResponse(ollamaResponse, request);
            
        } catch (Exception e) {
            log.error("Ollama generation failed for model: {}", modelName, e);
            throw new LLMGenerationException("Failed to generate completion", e);
        }
    }
    
    @Override
    public LLMResponse generateStreamingCompletion(LLMRequest request, 
                                                 StreamingResponseHandler handler) {
        String modelName = request.getModel() != null ? request.getModel() : defaultModel;
        
        try {
            String fullContext = contextManager.buildContext(request);
            
            OllamaGenerateRequest ollamaRequest = OllamaGenerateRequest.builder()
                .model(modelName)
                .prompt(fullContext)
                .temperature(request.getTemperature())
                .stream(true)
                .build();
            
            // Execute streaming request
            ollamaClient.generateStreaming(ollamaRequest, response -> {
                LLMStreamChunk chunk = responseProcessor.processStreamChunk(response);
                handler.onChunk(chunk);
            });
            
            return LLMResponse.streaming();
            
        } catch (Exception e) {
            log.error("Ollama streaming failed for model: {}", modelName, e);
            throw new LLMGenerationException("Failed to generate streaming completion", e);
        }
    }
    
    @Override
    public List<EmbeddingVector> generateEmbeddings(List<String> texts, String model) {
        try {
            List<EmbeddingVector> embeddings = new ArrayList<>();
            
            for (String text : texts) {
                OllamaEmbedRequest embedRequest = OllamaEmbedRequest.builder()
                    .model(model != null ? model : "nomic-embed-text")
                    .prompt(text)
                    .build();
                
                OllamaEmbedResponse embedResponse = ollamaClient.embed(embedRequest);
                embeddings.add(new EmbeddingVector(embedResponse.getEmbedding()));
            }
            
            return embeddings;
            
        } catch (Exception e) {
            log.error("Ollama embedding generation failed", e);
            throw new LLMGenerationException("Failed to generate embeddings", e);
        }
    }
    
    private OllamaGenerateResponse executeWithRetry(OllamaGenerateRequest request) {
        int maxRetries = 3;
        int attempt = 0;
        
        while (attempt < maxRetries) {
            try {
                return ollamaClient.generate(request);
            } catch (Exception e) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw e;
                }
                
                // Exponential backoff
                try {
                    Thread.sleep(1000 * (long) Math.pow(2, attempt));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }
        
        throw new RuntimeException("Max retries exceeded");
    }
    
    private void loadAvailableModels() {
        try {
            OllamaListModelsResponse response = ollamaClient.listModels();
            List<String> modelNames = response.getModels().stream()
                .map(OllamaModel::getName)
                .collect(Collectors.toList());
            
            modelManager.updateAvailableModels(modelNames);
            
        } catch (Exception e) {
            log.warn("Failed to load Ollama models, using default configuration", e);
            modelManager.updateAvailableModels(List.of(defaultModel));
        }
    }
}
```

### 2. Model Manager

```java
@Component
public class ModelManager {
    
    private final Map<String, ModelInfo> availableModels = new ConcurrentHashMap<>();
    private final Map<String, ModelPerformanceMetrics> modelMetrics = new ConcurrentHashMap<>();
    private final LoadBalancer modelLoadBalancer;
    
    @Autowired
    public ModelManager(LoadBalancer modelLoadBalancer) {
        this.modelLoadBalancer = modelLoadBalancer;
    }
    
    public void updateAvailableModels(List<String> modelNames) {
        // Clear existing models
        availableModels.clear();
        
        // Add new models with default configuration
        for (String modelName : modelNames) {
            ModelInfo modelInfo = ModelInfo.builder()
                .name(modelName)
                .type(detectModelType(modelName))
                .maxTokens(detectMaxTokens(modelName))
                .supportStreaming(true)
                .supportEmbeddings(detectEmbeddingSupport(modelName))
                .available(true)
                .lastHealthCheck(LocalDateTime.now())
                .build();
            
            availableModels.put(modelName, modelInfo);
            
            // Initialize performance metrics
            modelMetrics.put(modelName, new ModelPerformanceMetrics(modelName));
        }
        
        log.info("Updated available models: {}", modelNames);
    }
    
    public String selectOptimalModel(LLMRequest request) {
        // Filter models based on request requirements
        List<ModelInfo> candidateModels = availableModels.values().stream()
            .filter(model -> model.isAvailable())
            .filter(model -> meetsRequirements(model, request))
            .collect(Collectors.toList());
        
        if (candidateModels.isEmpty()) {
            throw new ModelNotAvailableException("No suitable models available for request");
        }
        
        // Use load balancer to select best model based on performance
        return modelLoadBalancer.selectModel(candidateModels, request);
    }
    
    public void recordModelPerformance(String modelName, long responseTime, boolean success) {
        ModelPerformanceMetrics metrics = modelMetrics.get(modelName);
        if (metrics != null) {
            metrics.recordExecution(responseTime, success);
        }
    }
    
    public ModelPerformanceMetrics getModelMetrics(String modelName) {
        return modelMetrics.get(modelName);
    }
    
    public boolean isModelAvailable(String modelName) {
        ModelInfo model = availableModels.get(modelName);
        return model != null && model.isAvailable();
    }
    
    public List<ModelInfo> getAvailableModels() {
        return new ArrayList<>(availableModels.values());
    }
    
    private boolean meetsRequirements(ModelInfo model, LLMRequest request) {
        // Check token limit
        if (request.getMaxTokens() > model.getMaxTokens()) {
            return false;
        }
        
        // Check streaming support
        if (request.isStreaming() && !model.isSupportStreaming()) {
            return false;
        }
        
        // Check embedding support
        if (request.getType() == LLMRequestType.EMBEDDING && !model.isSupportEmbeddings()) {
            return false;
        }
        
        return true;
    }
    
    private ModelType detectModelType(String modelName) {
        if (modelName.contains("code")) {
            return ModelType.CODE;
        } else if (modelName.contains("embed")) {
            return ModelType.EMBEDDING;
        } else {
            return ModelType.GENERAL;
        }
    }
    
    private int detectMaxTokens(String modelName) {
        // Default token limits based on model name patterns
        if (modelName.contains("7b")) {
            return 4096;
        } else if (modelName.contains("13b")) {
            return 8192;
        } else if (modelName.contains("34b") || modelName.contains("70b")) {
            return 16384;
        } else {
            return 2048; // Conservative default
        }
    }
    
    private boolean detectEmbeddingSupport(String modelName) {
        return modelName.contains("embed") || modelName.contains("sentence");
    }
}
```

### 3. Context Manager

```java
@Service
public class ContextManager {
    
    private final ConversationRepository conversationRepository;
    private final ContextCache contextCache;
    private final TokenCounter tokenCounter;
    
    @Value("${llm.context.max-tokens:4000}")
    private int maxContextTokens;
    
    @Value("${llm.context.history-window:10}")
    private int conversationHistoryWindow;
    
    public String buildContext(LLMRequest request) {
        StringBuilder contextBuilder = new StringBuilder();
        
        // Add system prompt if provided
        if (request.getSystemPrompt() != null) {
            contextBuilder.append("System: ").append(request.getSystemPrompt()).append("\n\n");
        }
        
        // Add conversation history
        if (request.getConversationId() != null) {
            List<ConversationMessage> history = getConversationHistory(request.getConversationId());
            for (ConversationMessage message : history) {
                contextBuilder.append(formatMessage(message)).append("\n");
            }
        }
        
        // Add current prompt
        contextBuilder.append("User: ").append(request.getPrompt()).append("\n");
        contextBuilder.append("Assistant: ");
        
        // Trim context if too long
        return trimContextToLimit(contextBuilder.toString());
    }
    
    public void saveConversationMessage(String conversationId, ConversationMessage message) {
        conversationRepository.saveMessage(conversationId, message);
        
        // Update cache
        contextCache.addMessage(conversationId, message);
        
        // Trim old messages if conversation is getting too long
        trimConversationHistory(conversationId);
    }
    
    private List<ConversationMessage> getConversationHistory(String conversationId) {
        // Try cache first
        List<ConversationMessage> cachedHistory = contextCache.getHistory(conversationId);
        if (cachedHistory != null) {
            return cachedHistory;
        }
        
        // Fall back to database
        List<ConversationMessage> dbHistory = conversationRepository.getMessages(
            conversationId, conversationHistoryWindow);
        
        // Update cache
        contextCache.setHistory(conversationId, dbHistory);
        
        return dbHistory;
    }
    
    private String formatMessage(ConversationMessage message) {
        String role = message.getRole();
        String content = message.getContent();
        
        return role + ": " + content;
    }
    
    private String trimContextToLimit(String context) {
        int tokenCount = tokenCounter.countTokens(context);
        
        if (tokenCount <= maxContextTokens) {
            return context;
        }
        
        // Trim from the beginning while preserving the latest user message
        String[] lines = context.split("\n");
        StringBuilder trimmedContext = new StringBuilder();
        
        // Always include the last user message and assistant prompt
        for (int i = lines.length - 3; i < lines.length; i++) {
            if (i >= 0) {
                trimmedContext.append(lines[i]).append("\n");
            }
        }
        
        // Add as much history as possible
        for (int i = lines.length - 4; i >= 0; i--) {
            String potentialContext = lines[i] + "\n" + trimmedContext.toString();
            if (tokenCounter.countTokens(potentialContext) <= maxContextTokens) {
                trimmedContext.insert(0, lines[i] + "\n");
            } else {
                break;
            }
        }
        
        return trimmedContext.toString();
    }
    
    private void trimConversationHistory(String conversationId) {
        int currentMessageCount = conversationRepository.getMessageCount(conversationId);
        
        if (currentMessageCount > conversationHistoryWindow * 2) {
            int messagesToDelete = currentMessageCount - conversationHistoryWindow;
            conversationRepository.deleteOldestMessages(conversationId, messagesToDelete);
            
            // Clear cache to force refresh
            contextCache.evictHistory(conversationId);
        }
    }
}
```

## Production Monitoring

### 1. Metrics Configuration

```java
@Configuration
@EnablePrometheusEndpoint
@EnableMetrics
public class MetricsConfiguration {
    
    @Bean
    public MeterRegistry meterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
    
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
    
    @Bean
    public CountedAspect countedAspect(MeterRegistry registry) {
        return new CountedAspect(registry);
    }
    
    @Bean
    @ConditionalOnProperty(name = "management.metrics.export.prometheus.enabled", havingValue = "true")
    public PrometheusPushGatewayManager prometheusPushGatewayManager(
            @Value("${management.metrics.export.prometheus.pushgateway.base-url:http://localhost:9091}") 
            String pushGatewayUrl,
            MeterRegistry meterRegistry) {
        
        return new PrometheusPushGatewayManager(pushGatewayUrl, meterRegistry);
    }
}
```

### 2. Custom Metrics

```java
@Component
@Slf4j
public class AgentSystemMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter agentRequestCounter;
    private final Timer agentResponseTimer;
    private final Gauge activeAgentsGauge;
    private final Counter llmRequestCounter;
    private final Timer llmResponseTimer;
    private final Counter pluginExecutionCounter;
    private final Timer workflowExecutionTimer;
    
    public AgentSystemMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Agent metrics
        this.agentRequestCounter = Counter.builder("agent_requests_total")
            .description("Total number of agent requests")
            .tag("type", "unknown")
            .register(meterRegistry);
        
        this.agentResponseTimer = Timer.builder("agent_response_duration")
            .description("Agent response time")
            .register(meterRegistry);
        
        this.activeAgentsGauge = Gauge.builder("agents_active")
            .description("Number of active agents")
            .register(meterRegistry, this, AgentSystemMetrics::getActiveAgentCount);
        
        // LLM metrics
        this.llmRequestCounter = Counter.builder("llm_requests_total")
            .description("Total number of LLM requests")
            .register(meterRegistry);
        
        this.llmResponseTimer = Timer.builder("llm_response_duration")
            .description("LLM response time")
            .register(meterRegistry);
        
        // Plugin metrics
        this.pluginExecutionCounter = Counter.builder("plugin_executions_total")
            .description("Total number of plugin executions")
            .register(meterRegistry);
        
        // Workflow metrics
        this.workflowExecutionTimer = Timer.builder("workflow_execution_duration")
            .description("Workflow execution time")
            .register(meterRegistry);
    }
    
    public void recordAgentRequest(String agentType, String operation) {
        agentRequestCounter.increment(
            Tags.of(
                Tag.of("agent_type", agentType),
                Tag.of("operation", operation)
            )
        );
    }
    
    public Timer.Sample startAgentTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordAgentResponse(Timer.Sample sample, String agentType, String status) {
        sample.stop(Timer.builder("agent_response_duration")
            .tag("agent_type", agentType)
            .tag("status", status)
            .register(meterRegistry));
    }
    
    public void recordLLMRequest(String model, String provider) {
        llmRequestCounter.increment(
            Tags.of(
                Tag.of("model", model),
                Tag.of("provider", provider)
            )
        );
    }
    
    public void recordLLMResponse(long duration, String model, boolean success) {
        llmResponseTimer.record(Duration.ofMillis(duration),
            Tags.of(
                Tag.of("model", model),
                Tag.of("success", String.valueOf(success))
            )
        );
    }
    
    public void recordPluginExecution(String pluginName, String agentId, boolean success) {
        pluginExecutionCounter.increment(
            Tags.of(
                Tag.of("plugin", pluginName),
                Tag.of("agent_id", agentId),
                Tag.of("success", String.valueOf(success))
            )
        );
    }
    
    public void recordWorkflowExecution(long duration, String workflowId, String status) {
        workflowExecutionTimer.record(Duration.ofMillis(duration),
            Tags.of(
                Tag.of("workflow_id", workflowId),
                Tag.of("status", status)
            )
        );
    }
    
    private double getActiveAgentCount() {
        // Implementation to count active agents
        return agentService.getActiveAgentCount();
    }
}
```

### 3. Health Checks

```java
@Component
public class SystemHealthIndicator implements HealthIndicator {
    
    private final AgentService agentService;
    private final LLMIntegrationService llmService;
    private final DatabaseHealthChecker databaseChecker;
    private final CacheHealthChecker cacheChecker;
    
    @Override
    public Health health() {
        Health.Builder healthBuilder = Health.up();
        
        // Check core components
        checkAgentService(healthBuilder);
        checkLLMService(healthBuilder);
        checkDatabase(healthBuilder);
        checkCache(healthBuilder);
        checkMemoryUsage(healthBuilder);
        checkDiskUsage(healthBuilder);
        
        return healthBuilder.build();
    }
    
    private void checkAgentService(Health.Builder builder) {
        try {
            int activeAgents = agentService.getActiveAgentCount();
            builder.withDetail("agents.active", activeAgents);
            
            if (activeAgents == 0) {
                builder.withDetail("agents.status", "WARNING: No active agents");
            } else {
                builder.withDetail("agents.status", "OK");
            }
        } catch (Exception e) {
            builder.down().withDetail("agents.error", e.getMessage());
        }
    }
    
    private void checkLLMService(Health.Builder builder) {
        try {
            boolean available = llmService.isHealthy();
            if (available) {
                builder.withDetail("llm.status", "OK");
                builder.withDetail("llm.models", llmService.getAvailableModels().size());
            } else {
                builder.down().withDetail("llm.status", "LLM service unavailable");
            }
        } catch (Exception e) {
            builder.down().withDetail("llm.error", e.getMessage());
        }
    }
    
    private void checkDatabase(Health.Builder builder) {
        try {
            boolean healthy = databaseChecker.isHealthy();
            if (healthy) {
                builder.withDetail("database.status", "OK");
                builder.withDetail("database.connections", databaseChecker.getActiveConnections());
            } else {
                builder.down().withDetail("database.status", "Database unreachable");
            }
        } catch (Exception e) {
            builder.down().withDetail("database.error", e.getMessage());
        }
    }
    
    private void checkCache(Health.Builder builder) {
        try {
            boolean healthy = cacheChecker.isHealthy();
            if (healthy) {
                builder.withDetail("cache.status", "OK");
                builder.withDetail("cache.hit_rate", cacheChecker.getHitRate());
            } else {
                builder.withDetail("cache.status", "WARNING: Cache unavailable");
            }
        } catch (Exception e) {
            builder.withDetail("cache.error", e.getMessage());
        }
    }
    
    private void checkMemoryUsage(Health.Builder builder) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        
        long used = heapUsage.getUsed();
        long max = heapUsage.getMax();
        double usagePercent = (double) used / max * 100;
        
        builder.withDetail("memory.used_mb", used / 1024 / 1024);
        builder.withDetail("memory.max_mb", max / 1024 / 1024);
        builder.withDetail("memory.usage_percent", Math.round(usagePercent * 100.0) / 100.0);
        
        if (usagePercent > 90) {
            builder.down().withDetail("memory.status", "CRITICAL: High memory usage");
        } else if (usagePercent > 80) {
            builder.withDetail("memory.status", "WARNING: High memory usage");
        } else {
            builder.withDetail("memory.status", "OK");
        }
    }
    
    private void checkDiskUsage(Health.Builder builder) {
        try {
            File root = new File("/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            double usagePercent = (double) usedSpace / totalSpace * 100;
            
            builder.withDetail("disk.used_gb", usedSpace / 1024 / 1024 / 1024);
            builder.withDetail("disk.free_gb", freeSpace / 1024 / 1024 / 1024);
            builder.withDetail("disk.usage_percent", Math.round(usagePercent * 100.0) / 100.0);
            
            if (usagePercent > 95) {
                builder.down().withDetail("disk.status", "CRITICAL: Disk space low");
            } else if (usagePercent > 85) {
                builder.withDetail("disk.status", "WARNING: Disk space low");
            } else {
                builder.withDetail("disk.status", "OK");
            }
        } catch (Exception e) {
            builder.withDetail("disk.error", e.getMessage());
        }
    }
}
```

## API Gateway Implementation

### 1. Gateway Configuration

```java
@RestController
@RequestMapping("/api/v1/gateway")
@Slf4j
public class APIGatewayController {
    
    private final RateLimitingService rateLimitingService;
    private final LoadBalancingService loadBalancingService;
    private final CircuitBreakerService circuitBreakerService;
    private final CacheService cacheService;
    
    @PostMapping("/agents/{agentId}/execute")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse> executeAgentTask(
            @PathVariable Long agentId,
            @RequestBody AgentTaskRequest request,
            HttpServletRequest httpRequest) {
        
        String clientId = getClientId(httpRequest);
        
        // Rate limiting
        if (!rateLimitingService.isAllowed(clientId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error("Rate limit exceeded"));
        }
        
        // Check cache for similar requests
        String cacheKey = generateCacheKey(agentId, request);
        ApiResponse cachedResponse = cacheService.get(cacheKey);
        if (cachedResponse != null) {
            return ResponseEntity.ok(cachedResponse);
        }
        
        try {
            // Circuit breaker protection
            ApiResponse response = circuitBreakerService.execute(
                "agent-" + agentId,
                () -> executeAgentTaskInternal(agentId, request)
            );
            
            // Cache successful responses
            if (response.isSuccess() && request.isCacheable()) {
                cacheService.put(cacheKey, response, Duration.ofMinutes(5));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (CircuitBreakerOpenException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Service temporarily unavailable"));
        } catch (Exception e) {
            log.error("Agent task execution failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error"));
        }
    }
    
    private ApiResponse executeAgentTaskInternal(Long agentId, AgentTaskRequest request) {
        // Load balance to available agent instances
        AgentInstance instance = loadBalancingService.selectInstance(agentId);
        
        // Execute task
        return instance.executeTask(request);
    }
    
    private String getClientId(HttpServletRequest request) {
        // Try to get authenticated user ID first
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        
        // Fall back to IP address
        return getClientIpAddress(request);
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private String generateCacheKey(Long agentId, AgentTaskRequest request) {
        return String.format("agent:%d:task:%s", agentId, 
            DigestUtils.md5Hex(request.toString()));
    }
}
```

This architecture provides a complete, production-ready multi-agent system with enterprise-grade features, monitoring, and scalability built on the foundation of all four phases.