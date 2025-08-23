# Phase 1: Foundation - Simple Spring Boot Agent

Welcome to Phase 1 of the multi-agent system implementation! In this phase, you'll build a basic autonomous agent that responds to natural language commands using Spring Boot.

## 🎯 Learning Objectives

By the end of this phase, you will have:

- ✅ A working Spring Boot application with REST API
- ✅ Basic agent entity with JPA persistence
- ✅ Natural language command processing
- ✅ H2 database integration
- ✅ Health checks and monitoring endpoints
- ✅ Comprehensive error handling and logging
- ✅ Unit and integration tests

## 🏗️ What You'll Build

### Core Components

1. **AgentController** - REST endpoints for agent interaction
2. **AgentService** - Business logic and command processing
3. **Agent Entity** - JPA entity for data persistence
4. **CommandProcessor** - Natural language command parsing
5. **HealthMonitor** - System health checks and metrics

### API Endpoints

```
GET    /api/v1/agents           # List all agents
POST   /api/v1/agents           # Create a new agent
GET    /api/v1/agents/{id}      # Get agent by ID
PUT    /api/v1/agents/{id}      # Update agent
DELETE /api/v1/agents/{id}      # Delete agent
POST   /api/v1/agents/{id}/execute  # Execute command

GET    /actuator/health         # Health check
GET    /actuator/metrics        # Application metrics
GET    /h2-console             # H2 database console
```

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Your favorite IDE

### Build and Run
```bash
# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run

# Test the API
curl -X POST http://localhost:8080/api/v1/agents \
  -H "Content-Type: application/json" \
  -d '{"name": "Assistant", "type": "GENERAL", "configuration": "{}"}'
```

## 📋 Implementation Checklist

### Step 1: Project Setup
- [ ] Create Spring Boot project structure
- [ ] Configure Maven dependencies
- [ ] Set up application properties
- [ ] Configure H2 database

### Step 2: Core Entities
- [ ] Create Agent entity
- [ ] Create Command entity
- [ ] Set up JPA repositories
- [ ] Configure database schema

### Step 3: Business Logic
- [ ] Implement AgentService
- [ ] Create CommandProcessor
- [ ] Add natural language parsing
- [ ] Implement agent state management

### Step 4: REST API
- [ ] Create AgentController
- [ ] Implement CRUD operations
- [ ] Add command execution endpoint
- [ ] Configure exception handling

### Step 5: Testing
- [ ] Write unit tests
- [ ] Create integration tests
- [ ] Add test data setup
- [ ] Configure test profiles

### Step 6: Monitoring
- [ ] Configure actuator endpoints
- [ ] Add custom health indicators
- [ ] Set up logging configuration
- [ ] Create metrics collection

## 🔧 Technology Stack

### Core Dependencies
- **Spring Boot 3.2+** - Main framework
- **Spring Web** - REST API
- **Spring Data JPA** - Data persistence
- **H2 Database** - Embedded database
- **Spring Boot Actuator** - Monitoring and health checks
- **Spring Boot Test** - Testing framework

### Testing Dependencies
- **JUnit 5** - Unit testing
- **Mockito** - Mocking framework
- **Testcontainers** - Integration testing
- **MockMvc** - Web layer testing

## 📖 Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Phase 1 Architecture                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐     │
│  │   REST API  │    │   Agent     │    │   Command   │     │
│  │ Controller  │◄───┤   Service   │◄───┤ Processor   │     │
│  │             │    │             │    │             │     │
│  └─────────────┘    └─────────────┘    └─────────────┘     │
│         ▲                   ▲                   ▲          │
│         │                   │                   │          │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐     │
│  │  Exception  │    │    Agent    │    │    Health   │     │
│  │  Handler    │    │   Entity    │    │   Monitor   │     │
│  │             │    │    (JPA)    │    │             │     │
│  └─────────────┘    └─────────────┘    └─────────────┘     │
│                             ▲                              │
│                             │                              │
│                    ┌─────────────┐                         │
│                    │     H2      │                         │
│                    │  Database   │                         │
│                    │             │                         │
│                    └─────────────┘                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 🎓 Learning Path

### 1. Start Here
- [📖 Phase 1 Walkthrough](../learning-path/phase1-walkthrough.md) - Step-by-step tutorial
- [🏗️ Architecture Deep Dive](architecture.md) - Technical details
- [🔧 Database Setup](database-setup.md) - H2 configuration

### 2. Implementation Guides
- [📝 API Documentation](api-guide.md) - REST endpoint details
- [🧪 Testing Strategy](testing-guide.md) - Test implementation

### 3. Next Steps
After completing Phase 1, move to:
- [Phase 2: Enhanced Capabilities](../phase2/README.md)

## 💡 Key Concepts

### Agent Model
```java
@Entity
@Table(name = "agents")
public class Agent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    private AgentType type;
    
    @Enumerated(EnumType.STRING)
    private AgentStatus status;
    
    @Column(columnDefinition = "TEXT")
    private String configuration;
    
    // Getters, setters, constructors...
}
```

### Command Processing
```java
@Service
public class CommandProcessor {
    
    public CommandResult processCommand(String command, String context) {
        // 1. Parse natural language command
        ParsedCommand parsed = parseCommand(command);
        
        // 2. Validate command
        if (!isValidCommand(parsed)) {
            throw new InvalidCommandException("Invalid command format");
        }
        
        // 3. Execute command logic
        Object result = executeCommand(parsed, context);
        
        // 4. Return structured result
        return CommandResult.success(result);
    }
}
```

### REST API Example
```java
@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {
    
    @PostMapping("/{id}/execute")
    public ResponseEntity<CommandResult> executeCommand(
            @PathVariable Long id,
            @RequestBody CommandRequest request) {
        
        try {
            CommandResult result = agentService.executeCommand(id, request);
            return ResponseEntity.ok(result);
        } catch (AgentNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
```

## 🚨 Common Pitfalls

### 1. Database Configuration
**Problem**: H2 console not accessible
**Solution**: Ensure H2 console is enabled in application.properties
```properties
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

### 2. JPA Entity Design
**Problem**: Lazy loading issues
**Solution**: Use `@Transactional` appropriately and consider fetch strategies

### 3. Error Handling
**Problem**: Unclear error messages
**Solution**: Implement global exception handling with meaningful responses

### 4. Testing Configuration
**Problem**: Tests interfere with each other
**Solution**: Use `@DirtiesContext` or proper test isolation

## 🔍 Debugging Tips

### 1. Enable Debug Logging
```properties
logging.level.com.jamesokeeffe.agentsystem=DEBUG
logging.level.org.springframework.web=DEBUG
```

### 2. H2 Console Access
```bash
# Start application and visit:
http://localhost:8080/h2-console

# Connection settings:
JDBC URL: jdbc:h2:mem:agentdb
Username: sa
Password: (leave empty)
```

### 3. Actuator Endpoints
```bash
# Health check
curl http://localhost:8080/actuator/health

# Application info
curl http://localhost:8080/actuator/info

# Metrics
curl http://localhost:8080/actuator/metrics
```

## 📚 Additional Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [H2 Database Documentation](http://www.h2database.com/html/main.html)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)

## ✅ Success Criteria

You've successfully completed Phase 1 when:

1. ✅ Your Spring Boot application starts without errors
2. ✅ You can create, read, update, and delete agents via REST API
3. ✅ You can execute commands and get meaningful responses
4. ✅ H2 database stores and retrieves data correctly
5. ✅ Health checks are working and reporting system status
6. ✅ All tests pass and provide good coverage
7. ✅ Error handling provides clear, actionable messages

**Ready to start?** Head to the [Phase 1 Walkthrough](../learning-path/phase1-walkthrough.md) for step-by-step implementation! 🚀