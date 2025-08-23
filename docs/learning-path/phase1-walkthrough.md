# Phase 1 Walkthrough - Build Your First Agent System

Welcome to the comprehensive Phase 1 walkthrough! This hands-on tutorial will guide you through building a complete autonomous agent system from scratch. By the end, you'll have a working multi-agent foundation ready for enhancement in future phases.

## 🎯 What You'll Build

In this walkthrough, you'll create:

- ✅ A Spring Boot application with REST API
- ✅ Autonomous agents that respond to natural language commands
- ✅ H2 database with agent and command persistence
- ✅ Natural language processing with pattern matching
- ✅ Complete CRUD operations for agent management
- ✅ Command execution with status tracking
- ✅ Health monitoring and metrics collection

**Time Required:** 2-3 hours
**Difficulty:** Beginner to Intermediate
**Prerequisites:** Java 17+, Maven, Basic Spring Boot knowledge

## 📋 Prerequisites Check

Before starting, ensure you have:

```bash
# Check Java version (should be 17+)
java -version

# Check Maven version (should be 3.6+)
mvn -version

# Check Git version
git --version
```

**Required Tools:**
- Java 17 or higher
- Maven 3.6 or higher
- Your favorite IDE (IntelliJ IDEA, VS Code, Eclipse)
- Git for version control
- cURL or Postman for API testing

## 🚀 Step 1: Project Setup

### 1.1 Clone and Initialize

```bash
# Clone the repository
git clone https://github.com/jamesokeeffe/agent-docs.git
cd agent-docs

# Verify the project structure
ls -la
```

You should see:
```
├── README.md
├── ARCHITECTURE.md
├── GETTING-STARTED.md
├── pom.xml
├── src/
├── docs/
└── mvnw
```

### 1.2 Understand the Project Structure

Let's explore the key directories:

```bash
# View the main source structure
tree src/main/java/com/jamesokeeffe/agentsystem/
```

**Key Components:**
- `model/` - Domain entities (Agent, Command)
- `repository/` - Data access layer
- `service/` - Business logic layer
- `controller/` - REST API endpoints

### 1.3 Build and Verify

```bash
# Clean and compile the project
./mvnw clean compile

# Run tests to ensure everything works
./mvnw test

# Start the application
./mvnw spring-boot:run
```

**Verify the application is running:**
```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Should return:
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

## 🗄️ Step 2: Understanding the Database

### 2.1 Access H2 Console

While the application is running:

1. Open your browser to: `http://localhost:8080/h2-console`
2. Use these connection settings:
   ```
   JDBC URL: jdbc:h2:mem:agentdb
   User Name: sa
   Password: (leave empty)
   ```
3. Click "Connect"

### 2.2 Explore the Schema

Execute these queries in the H2 console:

```sql
-- View all tables
SHOW TABLES;

-- Examine agents table structure
DESCRIBE agents;

-- Examine commands table structure  
DESCRIBE commands;

-- Check if any data exists (should be empty initially)
SELECT COUNT(*) FROM agents;
SELECT COUNT(*) FROM commands;
```

**Understanding the Schema:**

The system uses two main tables:
- `agents`: Stores agent definitions and metadata
- `commands`: Stores command execution history

Each agent can have multiple commands (one-to-many relationship).

### 2.3 Database Configuration

Review the database configuration in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:agentdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  h2:
    console:
      enabled: true
      path: /h2-console
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
```

**Key Points:**
- In-memory database (`mem:agentdb`)
- Tables created/dropped on startup/shutdown
- SQL statements logged for debugging
- H2 console enabled for development

## 🤖 Step 3: Creating Your First Agent

### 3.1 Create Agent via API

Let's create your first agent using the REST API:

```bash
# Create a general-purpose agent
curl -X POST http://localhost:8080/api/v1/agents \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MyFirstAgent",
    "type": "GENERAL",
    "description": "My very first autonomous agent"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "MyFirstAgent",
    "type": "GENERAL",
    "status": "IDLE",
    "description": "My very first autonomous agent",
    "commandCount": 0,
    "createdAt": "2024-01-01T10:00:00Z",
    "updatedAt": "2024-01-01T10:00:00Z"
  },
  "message": "Agent created successfully"
}
```

### 3.2 Verify in Database

Go back to the H2 console and check:

```sql
-- View the newly created agent
SELECT * FROM agents;

-- Should show your agent with ID 1, status 'IDLE'
```

### 3.3 Understanding Agent Types

The system supports different agent types:

| Type | Description | Available in Phase |
|------|-------------|-------------------|
| `GENERAL` | General-purpose tasks | 1 |
| `CODE_REVIEWER` | Code analysis | 2+ |
| `DOCUMENTATION` | Document generation | 2+ |
| `TESTING` | Test creation | 2+ |
| `ORCHESTRATOR` | Workflow coordination | 3+ |
| `MONITOR` | System monitoring | 3+ |

For Phase 1, we primarily use `GENERAL` agents.

### 3.4 Create Multiple Agents

Create a few more agents for testing:

```bash
# Code-focused agent
curl -X POST http://localhost:8080/api/v1/agents \
  -H "Content-Type: application/json" \
  -d '{
    "name": "CodeHelper",
    "type": "GENERAL", 
    "description": "Helps with code-related tasks"
  }'

# Documentation agent
curl -X POST http://localhost:8080/api/v1/agents \
  -H "Content-Type: application/json" \
  -d '{
    "name": "DocWriter",
    "type": "GENERAL",
    "description": "Assists with documentation"
  }'
```

### 3.5 List All Agents

```bash
# Get all agents
curl http://localhost:8080/api/v1/agents
```

You should see a paginated list of your agents.

## 💬 Step 4: Command Execution

### 4.1 Your First Command

Let's execute a command on your first agent:

```bash
# Execute a greeting command
curl -X POST http://localhost:8080/api/v1/agents/1/execute \
  -H "Content-Type: application/json" \
  -d '{
    "command": "Hello, what can you help me with?",
    "context": "First interaction with my agent"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "commandId": 1,
    "status": "COMPLETED",
    "result": "Hello! I'm your agent assistant. I'm ready to help you with various tasks. You can ask me to analyze, create, explain, or provide information about different topics.",
    "executionTimeMs": 15,
    "timestamp": "2024-01-01T10:05:00Z",
    "success": true
  },
  "message": "Command executed successfully"
}
```

### 4.2 Understanding Command Processing

The system recognizes different command intents:

**Try different types of commands:**

```bash
# Help request
curl -X POST http://localhost:8080/api/v1/agents/1/execute \
  -H "Content-Type: application/json" \
  -d '{"command": "I need help with my project"}'

# Analysis request
curl -X POST http://localhost:8080/api/v1/agents/1/execute \
  -H "Content-Type: application/json" \
  -d '{
    "command": "Please analyze this code",
    "context": "function add(a, b) { return a + b; }"
  }'

# Status check
curl -X POST http://localhost:8080/api/v1/agents/1/execute \
  -H "Content-Type: application/json" \
  -d '{"command": "What is your current status?"}'

# Creation request
curl -X POST http://localhost:8080/api/v1/agents/1/execute \
  -H "Content-Type: application/json" \
  -d '{"command": "Create a simple function for me"}'
```

### 4.3 Command Intent Recognition

The system uses pattern matching to recognize intents:

| Pattern | Intent | Response Type |
|---------|--------|---------------|
| hello, hi, hey | greeting | Welcome message |
| help, assist | help | Available commands |
| status, health | status | System status |
| analyze, review | analyze | Analysis response |
| create, generate | create | Creation guidance |
| explain, describe | explain | Explanation |

### 4.4 Monitor Command History

```bash
# Get command history for agent 1
curl http://localhost:8080/api/v1/agents/1/commands
```

**Check in Database:**
```sql
-- View all commands
SELECT c.id, a.name as agent_name, c.content, c.status, c.timestamp 
FROM commands c 
JOIN agents a ON c.agent_id = a.id 
ORDER BY c.timestamp DESC;
```

## 📊 Step 5: Exploring Agent Analytics

### 5.1 Agent Statistics

```bash
# Get statistics for agent 1
curl http://localhost:8080/api/v1/agents/1/statistics
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "agentId": 1,
    "totalCommands": 4,
    "successfulCommands": 4,
    "failedCommands": 0,
    "successRate": 1.0
  },
  "message": "Agent statistics retrieved"
}
```

### 5.2 Query Agents

**Get agents by status:**
```bash
# Get all idle agents
curl http://localhost:8080/api/v1/agents/status/IDLE

# Get all available agents (shortcut for IDLE)
curl http://localhost:8080/api/v1/agents/available
```

**Get agents by type:**
```bash
# Get all general agents
curl http://localhost:8080/api/v1/agents/type/GENERAL
```

### 5.3 Performance Analysis

Execute this SQL query in H2 console for detailed analysis:

```sql
-- Agent performance summary
SELECT 
    a.name,
    a.status,
    COUNT(c.id) as total_commands,
    COUNT(CASE WHEN c.status = 'COMPLETED' THEN 1 END) as successful,
    COUNT(CASE WHEN c.status = 'FAILED' THEN 1 END) as failed,
    ROUND(AVG(c.execution_time_ms), 2) as avg_execution_time_ms,
    MAX(c.timestamp) as last_command
FROM agents a 
LEFT JOIN commands c ON a.id = c.agent_id 
GROUP BY a.id, a.name, a.status
ORDER BY successful DESC;
```

## 🔧 Step 6: Advanced Operations

### 6.1 Update Agent Configuration

```bash
# Update agent description and configuration
curl -X PUT http://localhost:8080/api/v1/agents/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MyFirstAgent",
    "type": "GENERAL",
    "description": "Updated: My enhanced autonomous agent",
    "configuration": "{\"maxMemory\": 2000, \"timeout\": 30}"
  }'
```

### 6.2 Test Error Handling

**Try creating a duplicate agent:**
```bash
curl -X POST http://localhost:8080/api/v1/agents \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MyFirstAgent",
    "type": "GENERAL"
  }'
```

**Expected Error:**
```json
{
  "success": false,
  "error": {
    "code": "AGENT_CREATION_FAILED",
    "message": "Agent with name 'MyFirstAgent' already exists"
  }
}
```

**Try invalid command:**
```bash
curl -X POST http://localhost:8080/api/v1/agents/999/execute \
  -H "Content-Type: application/json" \
  -d '{"command": "test"}'
```

### 6.3 Concurrent Command Testing

Let's test multiple simultaneous commands:

```bash
# Run multiple commands concurrently (use separate terminals or background processes)
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/v1/agents/1/execute \
    -H "Content-Type: application/json" \
    -d "{\"command\": \"Test command $i\"}" &
done

# Wait for all to complete
wait

# Check command history
curl http://localhost:8080/api/v1/agents/1/commands
```

## 🧪 Step 7: Testing Your Implementation

### 7.1 Manual Testing Checklist

**Agent Management:**
- [ ] Create agent with valid data
- [ ] Create agent with invalid data (should fail)
- [ ] Update agent information
- [ ] Delete agent
- [ ] List agents with pagination

**Command Execution:**
- [ ] Execute greeting command
- [ ] Execute help command
- [ ] Execute analysis command
- [ ] Execute creation command
- [ ] Execute unknown command

**Error Scenarios:**
- [ ] Command on non-existent agent
- [ ] Invalid command format
- [ ] Empty command content

### 7.2 Automated Testing

```bash
# Run the test suite
./mvnw test

# Run with coverage
./mvnw test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### 7.3 Performance Testing

Create a simple performance test script:

```bash
#!/bin/bash
# performance-test.sh

BASE_URL="http://localhost:8080/api/v1/agents"

# Create test agent
AGENT_RESPONSE=$(curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{"name": "PerfTestAgent", "type": "GENERAL"}')

AGENT_ID=$(echo $AGENT_RESPONSE | grep -o '"id":[0-9]*' | cut -d':' -f2)

echo "Created agent with ID: $AGENT_ID"

# Time command execution
start_time=$(date +%s%N)

for i in {1..10}; do
  curl -s -X POST "$BASE_URL/$AGENT_ID/execute" \
    -H "Content-Type: application/json" \
    -d "{\"command\": \"Performance test $i\"}" > /dev/null
done

end_time=$(date +%s%N)
execution_time=$((($end_time - $start_time) / 1000000))

echo "Executed 10 commands in $execution_time ms"
echo "Average time per command: $(($execution_time / 10)) ms"

# Cleanup
curl -s -X DELETE "$BASE_URL/$AGENT_ID"
echo "Test completed and cleaned up"
```

```bash
# Make it executable and run
chmod +x performance-test.sh
./performance-test.sh
```

## 📈 Step 8: Monitoring and Observability

### 8.1 Health Checks

**System Health:**
```bash
curl http://localhost:8080/actuator/health
```

**Detailed Health:**
```bash
curl http://localhost:8080/actuator/health/db
```

### 8.2 Metrics

**Available Metrics:**
```bash
curl http://localhost:8080/actuator/metrics
```

**Specific Metrics:**
```bash
# JVM memory usage
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP requests
curl http://localhost:8080/actuator/metrics/http.server.requests

# Database connections
curl http://localhost:8080/actuator/metrics/hikaricp.connections
```

### 8.3 Application Information

```bash
curl http://localhost:8080/actuator/info
```

## 🎯 Step 9: Customization Exercises

### 9.1 Add Custom Command Types

**Exercise:** Add support for a new command intent "calculate"

1. Open `CommandProcessor.java`
2. Add a new pattern to `COMMAND_PATTERNS`:
   ```java
   COMMAND_PATTERNS.put(Pattern.compile("(?i).*(calculate|compute|math).*"), "calculate");
   ```
3. Add a handler method:
   ```java
   private String handleCalculate(String command, String context) {
       String target = extractTarget(command, "calculate");
       return String.format("Calculation request for '%s': In Phase 1, I can provide basic calculation guidance. " +
                           "Advanced mathematical processing will be available in future phases.", target);
   }
   ```
4. Add the case to the switch statement in `executeCommand`

**Test your enhancement:**
```bash
curl -X POST http://localhost:8080/api/v1/agents/1/execute \
  -H "Content-Type: application/json" \
  -d '{"command": "Please calculate 2 + 2"}'
```

### 9.2 Add Agent Configuration Support

**Exercise:** Use the agent configuration field

1. Create a configuration class:
   ```java
   public class AgentConfiguration {
       private int maxMemorySize = 1000;
       private int commandTimeout = 30;
       private String nlpProvider = "mock";
       
       // getters and setters
   }
   ```

2. Update `AgentService` to parse and use configuration
3. Test with different configurations

### 9.3 Enhanced Command Context

**Exercise:** Make the system use command context more effectively

1. Modify `CommandProcessor` to analyze context
2. Provide context-aware responses
3. Store context relationships in database

## 🚀 Step 10: Preparing for Phase 2

### 10.1 Understanding the Foundation

You've now built a complete Phase 1 system with:

- ✅ **Agent Management**: Full CRUD operations
- ✅ **Command Processing**: Natural language understanding
- ✅ **Data Persistence**: H2 database with relationships
- ✅ **REST API**: Complete HTTP interface
- ✅ **Error Handling**: Comprehensive error responses
- ✅ **Monitoring**: Health checks and metrics
- ✅ **Testing**: Unit and integration tests

### 10.2 Architecture Review

**Current Architecture:**
```
[REST API] → [Service Layer] → [Repository Layer] → [H2 Database]
     ↑              ↑                ↑
[Error Handling] [Command Processing] [JPA Entities]
```

**What's Next in Phase 2:**
- AI API integration (OpenAI/Anthropic)
- Plugin system architecture
- JWT authentication and security
- Redis for caching and queuing
- Inter-agent communication

### 10.3 Code Quality Check

**Run Final Checks:**
```bash
# Compile and test
./mvnw clean compile test

# Check code coverage
./mvnw jacoco:report
```

**Review Checklist:**
- [ ] All tests passing
- [ ] Code coverage >80%
- [ ] No compiler warnings
- [ ] Application starts cleanly
- [ ] All endpoints working
- [ ] Database schema correct
- [ ] Error handling working

## 🎉 Congratulations!

You've successfully completed Phase 1! You now have:

### What You Built
- 🤖 **Autonomous Agent System** with natural language processing
- 🗄️ **Complete Database** with agents and command history
- 🌐 **REST API** with full CRUD operations
- 📊 **Analytics** with command statistics and performance metrics
- 🔍 **Monitoring** with health checks and application metrics
- 🧪 **Testing** with comprehensive test suite

### Skills Gained
- Spring Boot application development
- REST API design and implementation
- Database design with JPA/Hibernate
- Natural language processing basics
- Testing strategies (unit, integration, e2e)
- Application monitoring and observability

### Next Steps

1. **Explore the Documentation:**
   - [Phase 1 Architecture](../phase1/architecture.md)
   - [API Guide](../phase1/api-guide.md)
   - [Database Setup](../phase1/database-setup.md)
   - [Testing Guide](../phase1/testing-guide.md)

2. **Prepare for Phase 2:**
   - Review [Phase 2 Overview](../phase2/README.md)
   - Set up AI API keys (OpenAI or Anthropic)
   - Learn about plugin architectures

3. **Continue Learning:**
   - Experiment with custom command types
   - Try different database configurations
   - Explore Spring Boot advanced features

### Quick Reference Commands

```bash
# Start the application
./mvnw spring-boot:run

# Create an agent
curl -X POST http://localhost:8080/api/v1/agents \
  -H "Content-Type: application/json" \
  -d '{"name": "MyAgent", "type": "GENERAL"}'

# Execute a command
curl -X POST http://localhost:8080/api/v1/agents/1/execute \
  -H "Content-Type: application/json" \
  -d '{"command": "Hello!"}'

# Check health
curl http://localhost:8080/actuator/health

# Access H2 console
http://localhost:8080/h2-console
```

**Ready for the next challenge?** Head to [Phase 2: Enhanced Capabilities](../phase2/README.md) to add AI integration and advanced features! 🚀