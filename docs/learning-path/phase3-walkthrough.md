# Phase 3 Walkthrough: MCP Protocol and Advanced Orchestration

## Introduction

This walkthrough guides you through implementing Phase 3: MCP and Protocol Integration. You'll build standardized communication protocols, advanced workflow orchestration, and sophisticated agent coordination.

## Prerequisites

- Completed Phase 2 walkthrough
- Understanding of JSON-RPC protocols
- Knowledge of asynchronous programming
- Familiarity with workflow orchestration concepts

## Learning Path

### Step 1: Implement MCP Protocol Foundation (60 minutes)

**Objective**: Build the core Model Context Protocol infrastructure.

1. **Add MCP Dependencies**
   
   Update `pom.xml`:
   ```xml
   <dependency>
       <groupId>com.fasterxml.jackson.core</groupId>
       <artifactId>jackson-databind</artifactId>
   </dependency>
   <dependency>
       <groupId>org.springframework</groupId>
       <artifactId>spring-websocket</artifactId>
   </dependency>
   <dependency>
       <groupId>org.springframework</groupId>
       <artifactId>spring-messaging</artifactId>
   </dependency>
   ```

2. **Create MCP Message Structure**
   
   ```java
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public class MCPRequest {
       private String jsonrpc = "2.0";
       private String id;
       private String method;
       private JsonNode params;
       
       // Constructors, getters, setters
   }
   
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public class MCPResponse {
       private String jsonrpc = "2.0";
       private String id;
       private JsonNode result;
       private MCPError error;
       
       public static MCPResponse success(Object result) {
           MCPResponse response = new MCPResponse();
           response.result = objectMapper.valueToTree(result);
           return response;
       }
       
       public static MCPResponse error(MCPErrorCode code, String message) {
           MCPResponse response = new MCPResponse();
           response.error = new MCPError(code.getCode(), message);
           return response;
       }
   }
   ```

3. **Implement MCP Server**
   
   Create the core MCP server following the architecture guide.

4. **Add Schema Validation**
   
   ```java
   @Component
   public class MCPSchemaValidator {
       
       private final ObjectMapper objectMapper;
       private final Map<String, JsonSchema> schemas;
       
       public void validate(MCPRequest request) throws MCPValidationException {
           JsonSchema schema = schemas.get(request.getMethod());
           if (schema != null) {
               Set<ValidationMessage> errors = schema.validate(request.getParams());
               if (!errors.isEmpty()) {
                   throw new MCPValidationException("Invalid request parameters");
               }
           }
       }
   }
   ```

**Test Your Implementation**:
```bash
# Test MCP server initialization
curl -X POST http://localhost:8080/api/v1/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "1",
    "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "clientInfo": {"name": "test-client", "version": "1.0.0"}
    }
  }'
```

### Step 2: Build MCP Tools (90 minutes)

**Objective**: Create a comprehensive set of MCP tools for various operations.

1. **File System Tool**
   
   Implement `FileSystemTool` with operations:
   ```java
   @Component
   public class FileSystemTool implements MCPTool {
       
       @Override
       public MCPToolResult execute(Map<String, Object> arguments) {
           String operation = (String) arguments.get("operation");
           
           switch (operation) {
               case "read":
                   return readFile(arguments);
               case "write":
                   return writeFile(arguments);
               case "list":
                   return listDirectory(arguments);
               default:
                   throw new MCPToolExecutionException("Unknown operation: " + operation);
           }
       }
   }
   ```

2. **Database Tool**
   
   Create `DatabaseTool` with secure SQL execution capabilities.

3. **API Client Tool**
   
   Build `APIClientTool` for external service integration.

4. **Git Tool**
   
   Implement Git operations:
   ```java
   @Component
   public class GitTool implements MCPTool {
       
       private MCPToolResult executeGitCommand(Map<String, Object> arguments) {
           String operation = (String) arguments.get("operation");
           String repository = (String) arguments.get("repository");
           
           try {
               Git git = Git.open(new File(repository));
               
               switch (operation) {
                   case "status":
                       return getGitStatus(git);
                   case "diff":
                       return getGitDiff(git, arguments);
                   case "log":
                       return getGitLog(git, arguments);
                   default:
                       throw new IllegalArgumentException("Unknown Git operation: " + operation);
               }
           } catch (Exception e) {
               throw new MCPToolExecutionException("Git operation failed", e);
           }
       }
   }
   ```

**Test Your Implementation**:
```bash
# Test file system tool
curl -X POST http://localhost:8080/api/v1/mcp/tools/filesystem \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "operation": "read",
    "path": "/tmp/test.txt",
    "encoding": "UTF-8"
  }'

# Test database tool
curl -X POST http://localhost:8080/api/v1/mcp/tools/database \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "operation": "query",
    "sql": "SELECT * FROM agents LIMIT 5"
  }'
```

### Step 3: Implement Workflow Engine (75 minutes)

**Objective**: Build a sophisticated workflow orchestration system.

1. **Workflow Definition Schema**
   
   Create JSON schema for workflow definitions:
   ```java
   @Entity
   @Table(name = "workflow_definitions")
   public class WorkflowDefinition {
       @Id
       private String id;
       
       private String name;
       private String description;
       private String version;
       
       @Column(columnDefinition = "TEXT")
       private String definitionJson;
       
       @CreationTimestamp
       private LocalDateTime createdAt;
   }
   ```

2. **Workflow Execution Engine**
   
   Implement the workflow engine with dependency resolution:
   ```java
   @Service
   public class WorkflowEngine {
       
       public WorkflowExecution executeWorkflow(String workflowId, Map<String, Object> context) {
           WorkflowDefinition workflow = workflowRepository.findById(workflowId)
               .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found: " + workflowId));
           
           String executionId = UUID.randomUUID().toString();
           
           WorkflowExecution execution = WorkflowExecution.builder()
               .id(executionId)
               .workflowId(workflowId)
               .status(WorkflowStatus.RUNNING)
               .context(context)
               .startTime(LocalDateTime.now())
               .build();
           
           // Execute workflow asynchronously
           CompletableFuture.runAsync(() -> executeWorkflowSteps(workflow, execution));
           
           return execution;
       }
   }
   ```

3. **Step Execution Logic**
   
   Implement step execution with dependency management:
   ```java
   private CompletableFuture<StepResult> executeStepWithDependencies(
           WorkflowStep step, 
           Map<String, CompletableFuture<StepResult>> stepFutures,
           WorkflowExecution execution) {
       
       // Get dependency futures
       List<CompletableFuture<StepResult>> dependencies = step.getDependencies().stream()
           .map(stepFutures::get)
           .filter(Objects::nonNull)
           .collect(Collectors.toList());
       
       // Execute after dependencies complete
       return CompletableFuture.allOf(dependencies.toArray(new CompletableFuture[0]))
           .thenCompose(v -> executeStep(step, execution));
   }
   ```

4. **Progress Tracking**
   
   Implement real-time progress tracking:
   ```java
   @Component
   public class ProgressTracker {
       
       private final Map<String, WorkflowProgress> activeExecutions = new ConcurrentHashMap<>();
       
       public void updateStepProgress(String executionId, String stepId, StepStatus status) {
           WorkflowProgress progress = activeExecutions.get(executionId);
           if (progress != null) {
               progress.updateStep(stepId, status);
               
               // Broadcast progress update
               messagingTemplate.convertAndSend("/topic/workflow/" + executionId, progress);
           }
       }
   }
   ```

**Test Your Implementation**:
```bash
# Create a workflow
curl -X POST http://localhost:8080/api/v1/workflows \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "code-review-workflow",
    "name": "Code Review Process",
    "steps": [
      {
        "id": "fetch-code",
        "type": "mcp-tool",
        "tool": "git",
        "parameters": {"operation": "diff", "branch": "main"}
      },
      {
        "id": "analyze-code",
        "type": "agent-task",
        "agent": "code-analyzer",
        "dependencies": ["fetch-code"]
      }
    ]
  }'

# Execute the workflow
curl -X POST http://localhost:8080/api/v1/workflows/code-review-workflow/execute \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"repository": "/path/to/repo"}'
```

### Step 4: Advanced Agent Coordination (60 minutes)

**Objective**: Build sophisticated multi-agent coordination patterns.

1. **Task Distribution Service**
   
   ```java
   @Service
   public class TaskDistributionService {
       
       public AgentAssignment assignTask(Task task) {
           // Find capable agents
           List<Agent> capableAgents = agentCapabilityService.getCapableAgents(
               task.getRequiredCapabilities());
           
           if (capableAgents.isEmpty()) {
               throw new NoCapableAgentException("No agents available for task");
           }
           
           // Apply load balancing
           Agent selectedAgent = loadBalancer.selectAgent(capableAgents, task);
           
           // Check resource availability
           if (!resourceManager.hasAvailableResources(selectedAgent, 
                   task.getResourceRequirements())) {
               return queueTaskForLaterExecution(task, capableAgents);
           }
           
           return createAssignment(task, selectedAgent);
       }
   }
   ```

2. **Consensus Mechanism**
   
   Implement agent consensus for decision making:
   ```java
   @Service
   public class ConsensusManager {
       
       public ConsensusResult reachConsensus(String topic, List<Agent> participants, 
                                           ConsensusRequest request) {
           String sessionId = UUID.randomUUID().toString();
           
           // Broadcast request to all participants
           Map<String, CompletableFuture<Vote>> voteFutures = new HashMap<>();
           
           for (Agent participant : participants) {
               CompletableFuture<Vote> voteFuture = requestVote(participant, request);
               voteFutures.put(participant.getId(), voteFuture);
           }
           
           // Wait for votes and analyze result
           return analyzeConsensusResult(voteFutures, request);
       }
   }
   ```

3. **Load Balancing**
   
   Implement intelligent agent load balancing:
   ```java
   @Component
   public class AgentLoadBalancer {
       
       public Agent selectAgent(List<Agent> candidates, Task task) {
           return candidates.stream()
               .min(Comparator.comparing(this::calculateLoad))
               .orElseThrow(() -> new NoAvailableAgentException("No agents available"));
       }
       
       private double calculateLoad(Agent agent) {
           AgentMetrics metrics = metricsService.getAgentMetrics(agent.getId());
           
           double cpuLoad = metrics.getCpuUtilization();
           double memoryLoad = metrics.getMemoryUtilization();
           double taskQueue = metrics.getQueueSize() / 100.0; // Normalize
           
           return (cpuLoad * 0.4) + (memoryLoad * 0.3) + (taskQueue * 0.3);
       }
   }
   ```

**Test Your Implementation**:
```bash
# Test task distribution
curl -X POST http://localhost:8080/api/v1/tasks/distribute \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "CODE_ANALYSIS",
    "priority": "HIGH",
    "requiredCapabilities": ["java", "security"],
    "data": {"code": "..."}
  }'

# Test consensus mechanism
curl -X POST http://localhost:8080/api/v1/consensus \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "deployment-approval",
    "participants": [1, 2, 3],
    "request": {"action": "deploy", "version": "1.2.0"}
  }'
```

## Hands-On Exercises

### Exercise 1: Custom MCP Tool
**Time**: 45 minutes

Build a custom MCP tool for Docker operations:

```java
@Component
public class DockerTool implements MCPTool {
    
    @Override
    public MCPToolResult execute(Map<String, Object> arguments) {
        String operation = (String) arguments.get("operation");
        
        switch (operation) {
            case "ps":
                return listContainers();
            case "images":
                return listImages();
            case "inspect":
                return inspectContainer((String) arguments.get("containerId"));
            default:
                throw new MCPToolExecutionException("Unknown Docker operation");
        }
    }
    
    private MCPToolResult listContainers() {
        // Docker API call to list containers
        // Return formatted result
    }
}
```

### Exercise 2: Complex Workflow
**Time**: 60 minutes

Create a comprehensive CI/CD workflow:

```json
{
  "id": "ci-cd-pipeline",
  "name": "CI/CD Pipeline",
  "steps": [
    {
      "id": "checkout",
      "type": "mcp-tool",
      "tool": "git",
      "parameters": {"operation": "checkout", "branch": "main"}
    },
    {
      "id": "test",
      "type": "mcp-tool",
      "tool": "docker",
      "parameters": {"operation": "run", "image": "maven:3.8", "command": "mvn test"},
      "dependencies": ["checkout"]
    },
    {
      "id": "security-scan",
      "type": "agent-task",
      "agent": "security-scanner",
      "dependencies": ["checkout"],
      "parallel": true
    },
    {
      "id": "build",
      "type": "mcp-tool",
      "tool": "docker",
      "parameters": {"operation": "build", "tag": "app:latest"},
      "dependencies": ["test", "security-scan"]
    },
    {
      "id": "deploy",
      "type": "agent-task",
      "agent": "deployment-manager",
      "dependencies": ["build"],
      "conditions": [
        {"type": "approval", "required": true},
        {"type": "branch", "value": "main"}
      ]
    }
  ]
}
```

### Exercise 3: Protocol Comparison
**Time**: 30 minutes

Compare MCP vs REST vs Custom protocols:

1. Implement the same functionality using all three approaches
2. Measure performance characteristics
3. Analyze trade-offs in complexity vs flexibility

```java
@RestController
@RequestMapping("/comparison")
public class ProtocolComparisonController {
    
    @PostMapping("/mcp")
    public ResponseEntity<MCPResponse> mcpEndpoint(@RequestBody MCPRequest request) {
        long startTime = System.currentTimeMillis();
        MCPResponse response = mcpServer.processRequest(request);
        long duration = System.currentTimeMillis() - startTime;
        
        // Record metrics
        metricsService.recordProtocolUsage("MCP", duration, response.getError() == null);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/rest")
    public ResponseEntity<Object> restEndpoint(@RequestBody Map<String, Object> request) {
        // REST implementation
    }
    
    @PostMapping("/custom")
    public ResponseEntity<Object> customEndpoint(@RequestBody CustomProtocolMessage message) {
        // Custom protocol implementation
    }
}
```

## Performance Optimization

### 1. Async Workflow Execution
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "workflowExecutor")
    public TaskExecutor workflowExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("workflow-");
        executor.initialize();
        return executor;
    }
}
```

### 2. MCP Response Caching
```java
@Component
public class MCPResponseCache {
    
    private final Cache<String, MCPResponse> cache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(Duration.ofMinutes(5))
        .build();
    
    public MCPResponse getCachedResponse(MCPRequest request) {
        String cacheKey = generateCacheKey(request);
        return cache.getIfPresent(cacheKey);
    }
}
```

### 3. Workflow State Persistence
```java
@Service
public class WorkflowStateManager {
    
    @Transactional
    public void saveWorkflowState(WorkflowExecution execution) {
        // Persist execution state for recovery
        workflowExecutionRepository.save(execution);
        
        // Save step states
        execution.getSteps().forEach(step -> 
            stepExecutionRepository.save(step));
    }
    
    public WorkflowExecution recoverWorkflow(String executionId) {
        // Recover workflow from persisted state
        return workflowExecutionRepository.findById(executionId)
            .orElseThrow(() -> new WorkflowNotFoundException("Execution not found"));
    }
}
```

## Testing Strategies

### Integration Testing
```java
@SpringBootTest
@TestPropertySource(properties = {
    "mcp.enabled=true",
    "workflow.engine.enabled=true"
})
class MCPWorkflowIntegrationTest {
    
    @Autowired
    private MCPServer mcpServer;
    
    @Autowired
    private WorkflowEngine workflowEngine;
    
    @Test
    void shouldExecuteWorkflowWithMCPTools() {
        // Create test workflow
        WorkflowDefinition workflow = createTestWorkflow();
        
        // Execute workflow
        WorkflowExecution execution = workflowEngine.executeWorkflow(
            workflow.getId(), Map.of("testData", "value"));
        
        // Wait for completion
        await().atMost(30, SECONDS)
            .until(() -> execution.getStatus() == WorkflowStatus.COMPLETED);
        
        // Verify results
        assertThat(execution.getSteps()).hasSize(2);
        assertThat(execution.getSteps().get(0).getStatus()).isEqualTo(StepStatus.COMPLETED);
    }
}
```

### Load Testing
```java
@Test
void shouldHandleConcurrentWorkflowExecutions() {
    int numberOfWorkflows = 50;
    List<CompletableFuture<WorkflowExecution>> futures = new ArrayList<>();
    
    for (int i = 0; i < numberOfWorkflows; i++) {
        CompletableFuture<WorkflowExecution> future = CompletableFuture.supplyAsync(() ->
            workflowEngine.executeWorkflow("test-workflow", Map.of("index", i)));
        futures.add(future);
    }
    
    // Wait for all to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    
    // Verify all completed successfully
    futures.forEach(future -> {
        WorkflowExecution execution = future.join();
        assertThat(execution.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
    });
}
```

## What You've Built

After completing Phase 3, you now have:

1. **MCP Protocol Implementation**: Standardized communication with 10+ tools
2. **Workflow Orchestration Engine**: JSON-defined multi-step process coordination
3. **Advanced Agent Coordination**: Sophisticated task distribution and consensus
4. **Production Monitoring**: Comprehensive metrics and observability
5. **Protocol Comparison Framework**: Analysis of different communication approaches

## Next Steps

You're now ready for **Phase 4: Local Multi-Agent Copilot System**, where you'll:

- Integrate with local LLM providers (Ollama, LocalAI)
- Build a complete production-ready system
- Add comprehensive monitoring and observability
- Create deployment packages and documentation
- Optimize for enterprise-scale performance

The MCP protocol and workflow orchestration you've built in Phase 3 provides the sophisticated communication and coordination needed for the complete Copilot system in Phase 4.