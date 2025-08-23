# Phase 2 Walkthrough: Building Multi-Agent Coordination

## Introduction

This walkthrough guides you through implementing Phase 2: Multi-Agent Coordination. You'll build on Phase 1's foundation to add JWT authentication, a plugin system, and agent specialization.

## Prerequisites

- Completed Phase 1 walkthrough
- Basic understanding of Spring Security
- Familiarity with JWT tokens
- Knowledge of plugin architectures

## Learning Path

### Step 1: Implement JWT Authentication (30 minutes)

**Objective**: Add secure authentication to protect agent operations.

1. **Add Security Dependencies**
   
   Update `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-security</artifactId>
   </dependency>
   <dependency>
       <groupId>io.jsonwebtoken</groupId>
       <artifactId>jjwt-api</artifactId>
       <version>0.11.5</version>
   </dependency>
   <dependency>
       <groupId>io.jsonwebtoken</groupId>
       <artifactId>jjwt-impl</artifactId>
       <version>0.11.5</version>
   </dependency>
   ```

2. **Create User Entity**
   
   ```java
   @Entity
   @Table(name = "users")
   public class User implements UserDetails {
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;
       
       @Column(unique = true, nullable = false)
       private String username;
       
       @Column(nullable = false)
       private String passwordHash;
       
       @Enumerated(EnumType.STRING)
       private UserRole role;
       
       // UserDetails implementation...
   }
   ```

3. **Implement JWT Service**
   
   Create `JwtTokenService` following the security guide example.

4. **Configure Security**
   
   Create `SecurityConfig` class with JWT filter chain.

**Test Your Implementation**:
```bash
# Register a user
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'

# Use the returned token
curl -X GET http://localhost:8080/api/v1/agents \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Step 2: Build Plugin System (45 minutes)

**Objective**: Create a flexible plugin architecture for extending agent capabilities.

1. **Define Plugin Interface**
   
   ```java
   public interface AgentPlugin {
       String getName();
       String getVersion();
       void initialize(PluginContext context) throws PluginException;
       PluginResult execute(PluginRequest request) throws PluginException;
       boolean isHealthy();
       void shutdown();
   }
   ```

2. **Create Plugin Manager**
   
   Implement `PluginManager` with plugin loading and execution capabilities.

3. **Build Built-in Plugins**
   
   Create three basic plugins:
   - `EchoPlugin`: Simple message echoing
   - `TimePlugin`: Date and time operations
   - `CalculatorPlugin`: Basic mathematical operations

4. **Add Plugin Configuration**
   
   Create database tables for plugin configurations and execution logs.

**Test Your Implementation**:
```bash
# List plugins for an agent
curl -X GET http://localhost:8080/api/v1/plugins/agent/1 \
  -H "Authorization: Bearer YOUR_TOKEN"

# Execute echo plugin
curl -X POST http://localhost:8080/api/v1/plugins/echo/test \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello, Plugin System!"}'
```

### Step 3: Create Specialized Agents (60 minutes)

**Objective**: Build domain-specific agents for different tasks.

1. **Code Reviewer Agent**
   
   ```java
   @Component
   public class CodeReviewerAgent extends BaseAgent {
       
       @Override
       public AgentResponse processCommand(String command, Map<String, Object> context) {
           if (command.toLowerCase().contains("review")) {
               return performCodeReview(context);
           }
           return super.processCommand(command, context);
       }
       
       private AgentResponse performCodeReview(Map<String, Object> context) {
           // Code review logic
           List<String> issues = analyzeCode((String) context.get("code"));
           
           return AgentResponse.builder()
               .agentId(getId())
               .response("Code review completed")
               .data(Map.of("issues", issues, "status", "reviewed"))
               .timestamp(LocalDateTime.now())
               .build();
       }
   }
   ```

2. **Documentation Generator Agent**
   
   Create an agent that generates documentation from code.

3. **Test Creator Agent**
   
   Implement an agent that generates unit tests.

4. **Agent Registry**
   
   Create a registry to manage specialized agents and their capabilities.

**Test Your Implementation**:
```bash
# Create specialized agents
curl -X POST http://localhost:8080/api/v1/agents \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Code Reviewer", "type": "CODE_REVIEWER", "description": "Analyzes code quality"}'

# Test code review
curl -X POST http://localhost:8080/api/v1/agents/2/command \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"command": "review this code", "context": {"code": "public class Test { }"}}'
```

### Step 4: Implement Inter-Agent Communication (45 minutes)

**Objective**: Enable agents to communicate and coordinate tasks.

1. **Message Bus**
   
   ```java
   @Component
   public class AgentMessageBus {
       
       private final Map<Long, Agent> registeredAgents = new ConcurrentHashMap<>();
       
       public void sendMessage(Long fromAgentId, Long toAgentId, AgentMessage message) {
           Agent targetAgent = registeredAgents.get(toAgentId);
           if (targetAgent != null) {
               targetAgent.receiveMessage(fromAgentId, message);
           }
       }
       
       public void broadcastMessage(Long fromAgentId, AgentMessage message) {
           registeredAgents.values().forEach(agent -> 
               agent.receiveMessage(fromAgentId, message));
       }
   }
   ```

2. **Agent Communication Interface**
   
   Add message handling capabilities to agents.

3. **Task Delegation**
   
   Implement task delegation between specialized agents.

**Test Your Implementation**:
```bash
# Send message between agents
curl -X POST http://localhost:8080/api/v1/agents/1/message \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"targetAgentId": 2, "message": "Please review this code", "data": {"code": "..."}}'
```

## Hands-On Exercises

### Exercise 1: Custom Plugin Development
**Time**: 30 minutes

Build a custom plugin that integrates with an external API:

```java
@Component
public class WeatherPlugin implements AgentPlugin {
    
    @Override
    public PluginResult execute(PluginRequest request) {
        String city = request.getParameter("city", String.class);
        
        // Call weather API
        WeatherData weather = weatherService.getWeather(city);
        
        return PluginResult.success(Map.of(
            "temperature", weather.getTemperature(),
            "description", weather.getDescription(),
            "city", city
        ));
    }
}
```

### Exercise 2: Agent Workflow
**Time**: 45 minutes

Create a workflow where multiple agents collaborate:

1. Code Reviewer Agent analyzes code
2. Test Creator Agent generates tests
3. Documentation Agent creates docs
4. Results are aggregated and returned

### Exercise 3: Security Testing
**Time**: 20 minutes

Test the security implementation:

1. Try accessing endpoints without authentication
2. Test role-based access control
3. Verify JWT token expiration and refresh

## Common Issues and Solutions

### Issue 1: JWT Token Not Working
```bash
# Check token format
echo "YOUR_JWT_TOKEN" | base64 -d

# Verify token in JWT debugger
# https://jwt.io/
```

### Issue 2: Plugin Loading Failures
```java
// Add debug logging
@Slf4j
public class PluginManager {
    public void loadPlugin(String pluginName) {
        try {
            log.debug("Loading plugin: {}", pluginName);
            // Plugin loading logic
        } catch (Exception e) {
            log.error("Failed to load plugin: {}", pluginName, e);
            throw new PluginException("Plugin load failed", e);
        }
    }
}
```

### Issue 3: Agent Communication Problems
- Check agent registration in message bus
- Verify message serialization
- Test with simple message types first

## Performance Optimizations

### 1. Plugin Caching
```java
@Service
public class PluginCacheService {
    
    private final Cache<String, PluginResult> resultCache = 
        Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();
    
    public PluginResult getCachedResult(String cacheKey) {
        return resultCache.getIfPresent(cacheKey);
    }
}
```

### 2. Async Agent Communication
```java
@Async
public CompletableFuture<AgentResponse> sendAsyncMessage(AgentMessage message) {
    return CompletableFuture.supplyAsync(() -> {
        return processMessage(message);
    });
}
```

## Testing Strategy

### Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class PluginManagerTest {
    
    @Mock
    private PluginConfigService configService;
    
    @InjectMocks
    private PluginManager pluginManager;
    
    @Test
    void shouldExecutePluginSuccessfully() {
        // Test plugin execution
        PluginRequest request = new PluginRequest(Map.of("message", "test"));
        PluginResult result = pluginManager.executePlugin("echo", 1L, request);
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsKey("response");
    }
}
```

### Integration Tests
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AgentIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldAuthenticateAndExecuteAgentCommand() {
        // Login and get token
        LoginRequest loginRequest = new LoginRequest("admin", "admin123");
        ResponseEntity<AuthResponse> authResponse = restTemplate.postForEntity(
            "/api/v1/auth/login", loginRequest, AuthResponse.class);
        
        String token = authResponse.getBody().getAccessToken();
        
        // Execute agent command with token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        
        // Test agent execution...
    }
}
```

## What You've Built

After completing Phase 2, you now have:

1. **Secure Multi-Agent System**: JWT-based authentication protecting all operations
2. **Extensible Plugin Architecture**: Framework for adding new capabilities
3. **Specialized Agents**: Domain-specific agents for different tasks
4. **Agent Communication**: Message passing and coordination capabilities
5. **Production Security**: Role-based access control and secure endpoints

## Next Steps

You're now ready for **Phase 3: MCP and Protocol Integration**, where you'll:

- Implement the Model Context Protocol (MCP)
- Build advanced workflow orchestration
- Create sophisticated agent coordination patterns
- Compare different communication protocols

The foundation you've built in Phase 2 provides the security and extensibility needed for the advanced features in Phase 3.