# Phase 1 Testing Guide - Comprehensive Testing Strategy

This guide covers the complete testing strategy for Phase 1, including unit tests, integration tests, API testing, and test automation best practices.

## 🎯 Testing Philosophy

Our testing approach follows the **Testing Pyramid** principle:

```
    ┌─────────────────────┐
    │   E2E Tests (Few)   │  ← End-to-End API Tests
    ├─────────────────────┤
    │ Integration Tests   │  ← Service + Database Tests
    │     (Some)          │
    ├─────────────────────┤
    │   Unit Tests        │  ← Individual Class Tests
    │     (Many)          │
    └─────────────────────┘
```

**Testing Principles:**
- **Fast Feedback**: Unit tests run in milliseconds
- **Isolated**: Each test is independent and can run alone
- **Repeatable**: Same results every time
- **Self-Validating**: Clear pass/fail criteria
- **Comprehensive**: Cover happy paths, edge cases, and error conditions

## 🏗️ Test Structure Overview

### Test Organization

```
src/test/java/com/jamesokeeffe/agentsystem/
├── unit/                           # Unit tests
│   ├── model/
│   │   ├── AgentTest.java
│   │   └── CommandTest.java
│   ├── service/
│   │   ├── AgentServiceTest.java
│   │   └── CommandProcessorTest.java
│   └── controller/
│       └── AgentControllerTest.java
├── integration/                    # Integration tests
│   ├── repository/
│   │   ├── AgentRepositoryIT.java
│   │   └── CommandRepositoryIT.java
│   ├── service/
│   │   └── AgentServiceIT.java
│   └── controller/
│       └── AgentControllerIT.java
└── e2e/                           # End-to-end tests
    └── AgentSystemE2ETest.java
```

### Test Naming Convention

**Pattern:** `MethodName_StateUnderTest_ExpectedBehavior`

**Examples:**
- `createAgent_ValidInput_ReturnsAgentDto`
- `executeCommand_AgentNotFound_ThrowsException`
- `updateAgent_DuplicateName_ThrowsIllegalArgumentException`

## 🧪 Unit Testing

### 1. Model/Entity Tests

**AgentTest.java**
```java
@ExtendWith(MockitoExtension.class)
class AgentTest {

    @Test
    void constructor_ValidNameAndType_CreatesAgent() {
        // Given
        String name = "TestAgent";
        AgentType type = AgentType.GENERAL;
        
        // When
        Agent agent = new Agent(name, type);
        
        // Then
        assertThat(agent.getName()).isEqualTo(name);
        assertThat(agent.getType()).isEqualTo(type);
        assertThat(agent.getStatus()).isEqualTo(AgentStatus.IDLE);
    }

    @Test
    void isAvailable_IdleStatus_ReturnsTrue() {
        // Given
        Agent agent = new Agent("Test", AgentType.GENERAL);
        
        // When
        boolean available = agent.isAvailable();
        
        // Then
        assertThat(available).isTrue();
    }

    @Test
    void isAvailable_BusyStatus_ReturnsFalse() {
        // Given
        Agent agent = new Agent("Test", AgentType.GENERAL);
        agent.markBusy();
        
        // When
        boolean available = agent.isAvailable();
        
        // Then
        assertThat(available).isFalse();
    }

    @Test
    void addCommand_ValidCommand_UpdatesCommandCount() {
        // Given
        Agent agent = new Agent("Test", AgentType.GENERAL);
        Command command = new Command(agent, "test command");
        
        // When
        agent.addCommand(command);
        
        // Then
        assertThat(agent.getCommandCount()).isEqualTo(1);
        assertThat(command.getAgent()).isEqualTo(agent);
    }

    @Test
    void equals_SameIdAndName_ReturnsTrue() {
        // Given
        Agent agent1 = new Agent("Test", AgentType.GENERAL);
        agent1.setId(1L);
        Agent agent2 = new Agent("Test", AgentType.GENERAL);
        agent2.setId(1L);
        
        // When & Then
        assertThat(agent1).isEqualTo(agent2);
    }
}
```

**CommandTest.java**
```java
@ExtendWith(MockitoExtension.class)
class CommandTest {

    @Test
    void constructor_ValidAgentAndContent_CreatesCommand() {
        // Given
        Agent agent = new Agent("Test", AgentType.GENERAL);
        String content = "test command";
        
        // When
        Command command = new Command(agent, content);
        
        // Then
        assertThat(command.getAgent()).isEqualTo(agent);
        assertThat(command.getContent()).isEqualTo(content);
        assertThat(command.getStatus()).isEqualTo(CommandStatus.PENDING);
    }

    @Test
    void markCompleted_ValidResult_UpdatesStatusAndResult() {
        // Given
        Agent agent = new Agent("Test", AgentType.GENERAL);
        Command command = new Command(agent, "test");
        String result = "success";
        Long executionTime = 100L;
        
        // When
        command.markCompleted(result, executionTime);
        
        // Then
        assertThat(command.getStatus()).isEqualTo(CommandStatus.COMPLETED);
        assertThat(command.getResult()).isEqualTo(result);
        assertThat(command.getExecutionTimeMs()).isEqualTo(executionTime);
        assertThat(command.isSuccessful()).isTrue();
    }

    @Test
    void markFailed_ErrorMessage_UpdatesStatusAndError() {
        // Given
        Agent agent = new Agent("Test", AgentType.GENERAL);
        Command command = new Command(agent, "test");
        String errorMessage = "execution failed";
        Long executionTime = 50L;
        
        // When
        command.markFailed(errorMessage, executionTime);
        
        // Then
        assertThat(command.getStatus()).isEqualTo(CommandStatus.FAILED);
        assertThat(command.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(command.getExecutionTimeMs()).isEqualTo(executionTime);
        assertThat(command.isSuccessful()).isFalse();
    }
}
```

### 2. Service Layer Tests

**CommandProcessorTest.java**
```java
@ExtendWith(MockitoExtension.class)
class CommandProcessorTest {

    private CommandProcessor commandProcessor;

    @BeforeEach
    void setUp() {
        commandProcessor = new CommandProcessor();
    }

    @Test
    void processCommand_GreetingCommand_ReturnsGreetingResponse() {
        // Given
        String command = "Hello, how are you?";
        String context = "test context";
        String nlpProvider = "mock";
        
        // When
        CommandResult result = commandProcessor.processCommand(command, context, nlpProvider);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatus()).isEqualTo(CommandStatus.COMPLETED);
        assertThat(result.getResult()).contains("Hello!");
        assertThat(result.getExecutionTimeMs()).isNotNull();
    }

    @Test
    void processCommand_HelpCommand_ReturnsHelpResponse() {
        // Given
        String command = "I need help with my project";
        
        // When
        CommandResult result = commandProcessor.processCommand(command, null, "mock");
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).contains("help you with");
    }

    @Test
    void processCommand_AnalyzeCommand_ReturnsAnalysisResponse() {
        // Given
        String command = "Please analyze this code snippet";
        String context = "function test() { return true; }";
        
        // When
        CommandResult result = commandProcessor.processCommand(command, context, "mock");
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).contains("Analysis of");
    }

    @Test
    void processCommand_EmptyCommand_ReturnsUnknownResponse() {
        // Given
        String command = "";
        
        // When
        CommandResult result = commandProcessor.processCommand(command, null, "mock");
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).contains("not sure how to handle");
    }

    @Test
    void isValidCommand_ValidCommand_ReturnsTrue() {
        // Given
        String command = "This is a valid command";
        
        // When
        boolean valid = commandProcessor.isValidCommand(command);
        
        // Then
        assertThat(valid).isTrue();
    }

    @Test
    void isValidCommand_NullCommand_ReturnsFalse() {
        // Given
        String command = null;
        
        // When
        boolean valid = commandProcessor.isValidCommand(command);
        
        // Then
        assertThat(valid).isFalse();
    }

    @Test
    void isValidCommand_TooLongCommand_ReturnsFalse() {
        // Given
        String command = "x".repeat(5001);
        
        // When
        boolean valid = commandProcessor.isValidCommand(command);
        
        // Then
        assertThat(valid).isFalse();
    }
}
```

**AgentServiceTest.java**
```java
@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentRepository agentRepository;
    
    @Mock
    private CommandRepository commandRepository;
    
    @Mock
    private CommandProcessor commandProcessor;
    
    @InjectMocks
    private AgentService agentService;

    @Test
    void createAgent_ValidAgent_ReturnsAgentDto() {
        // Given
        AgentDto inputDto = new AgentDto("TestAgent", AgentType.GENERAL);
        Agent savedAgent = new Agent("TestAgent", AgentType.GENERAL);
        savedAgent.setId(1L);
        
        when(agentRepository.existsByName("TestAgent")).thenReturn(false);
        when(agentRepository.count()).thenReturn(0L);
        when(agentRepository.save(any(Agent.class))).thenReturn(savedAgent);
        
        // When
        AgentDto result = agentService.createAgent(inputDto);
        
        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("TestAgent");
        assertThat(result.getType()).isEqualTo(AgentType.GENERAL);
        assertThat(result.getStatus()).isEqualTo(AgentStatus.IDLE);
        
        verify(agentRepository).save(any(Agent.class));
    }

    @Test
    void createAgent_DuplicateName_ThrowsException() {
        // Given
        AgentDto inputDto = new AgentDto("ExistingAgent", AgentType.GENERAL);
        
        when(agentRepository.existsByName("ExistingAgent")).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> agentService.createAgent(inputDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void executeCommand_ValidCommand_ReturnsSuccess() {
        // Given
        Long agentId = 1L;
        Agent agent = new Agent("Test", AgentType.GENERAL);
        agent.setId(agentId);
        
        CommandRequest request = new CommandRequest("Hello");
        CommandResult processorResult = CommandResult.success("Hello response");
        processorResult.setExecutionTimeMs(10L);
        
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(commandProcessor.isValidCommand("Hello")).thenReturn(true);
        when(commandProcessor.processCommand(anyString(), any(), any()))
            .thenReturn(processorResult);
        when(commandRepository.save(any(Command.class)))
            .thenAnswer(invocation -> {
                Command cmd = invocation.getArgument(0);
                cmd.setId(1L);
                return cmd;
            });
        
        // When
        CommandResult result = agentService.executeCommand(agentId, request);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCommandId()).isEqualTo(1L);
        
        verify(agentRepository, times(2)).save(agent); // Once for busy, once for idle
        verify(commandRepository, times(2)).save(any(Command.class)); // Once initial, once with result
    }

    @Test
    void executeCommand_AgentNotFound_ThrowsException() {
        // Given
        Long agentId = 999L;
        CommandRequest request = new CommandRequest("Hello");
        
        when(agentRepository.findById(agentId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> agentService.executeCommand(agentId, request))
            .isInstanceOf(AgentNotFoundException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void executeCommand_AgentBusy_ThrowsException() {
        // Given
        Long agentId = 1L;
        Agent busyAgent = new Agent("Test", AgentType.GENERAL);
        busyAgent.setId(agentId);
        busyAgent.markBusy();
        
        CommandRequest request = new CommandRequest("Hello");
        
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(busyAgent));
        
        // When & Then
        assertThatThrownBy(() -> agentService.executeCommand(agentId, request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not available");
    }
}
```

### 3. Controller Layer Tests

**AgentControllerTest.java**
```java
@WebMvcTest(AgentController.class)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AgentService agentService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAgent_ValidInput_ReturnsCreated() throws Exception {
        // Given
        AgentDto inputDto = new AgentDto("TestAgent", AgentType.GENERAL);
        AgentDto responseDto = new AgentDto("TestAgent", AgentType.GENERAL);
        responseDto.setId(1L);
        responseDto.setStatus(AgentStatus.IDLE);
        
        when(agentService.createAgent(any(AgentDto.class))).thenReturn(responseDto);
        
        // When & Then
        mockMvc.perform(post("/api/v1/agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("TestAgent"))
                .andExpect(jsonPath("$.data.type").value("GENERAL"))
                .andExpect(jsonPath("$.message").value("Agent created successfully"));
        
        verify(agentService).createAgent(any(AgentDto.class));
    }

    @Test
    void createAgent_InvalidInput_ReturnsBadRequest() throws Exception {
        // Given
        AgentDto invalidDto = new AgentDto("", AgentType.GENERAL); // Empty name
        
        // When & Then
        mockMvc.perform(post("/api/v1/agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void executeCommand_ValidCommand_ReturnsSuccess() throws Exception {
        // Given
        Long agentId = 1L;
        CommandRequest request = new CommandRequest("Hello");
        CommandResult result = CommandResult.success("Hello response");
        result.setCommandId(1L);
        
        when(agentService.executeCommand(eq(agentId), any(CommandRequest.class)))
            .thenReturn(result);
        
        // When & Then
        mockMvc.perform(post("/api/v1/agents/{id}/execute", agentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.commandId").value(1))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    void getAgentById_ExistingAgent_ReturnsAgent() throws Exception {
        // Given
        Long agentId = 1L;
        AgentDto agentDto = new AgentDto("TestAgent", AgentType.GENERAL);
        agentDto.setId(agentId);
        
        when(agentService.getAgentById(agentId)).thenReturn(agentDto);
        
        // When & Then
        mockMvc.perform(get("/api/v1/agents/{id}", agentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("TestAgent"));
    }

    @Test
    void getAgentById_NonExistentAgent_ReturnsNotFound() throws Exception {
        // Given
        Long agentId = 999L;
        
        when(agentService.getAgentById(agentId))
            .thenThrow(new AgentNotFoundException("Agent not found"));
        
        // When & Then
        mockMvc.perform(get("/api/v1/agents/{id}", agentId))
                .andExpect(status().isNotFound());
    }
}
```

## 🔗 Integration Testing

### 1. Repository Integration Tests

**AgentRepositoryIT.java**
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AgentRepositoryIT {

    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private AgentRepository agentRepository;

    @Test
    void findByName_ExistingAgent_ReturnsAgent() {
        // Given
        Agent agent = new Agent("TestAgent", AgentType.GENERAL);
        entityManager.persistAndFlush(agent);
        
        // When
        Optional<Agent> found = agentRepository.findByName("TestAgent");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("TestAgent");
    }

    @Test
    void findByStatus_MultipleAgents_ReturnsCorrectAgents() {
        // Given
        Agent idleAgent = new Agent("IdleAgent", AgentType.GENERAL);
        Agent busyAgent = new Agent("BusyAgent", AgentType.GENERAL);
        busyAgent.markBusy();
        
        entityManager.persist(idleAgent);
        entityManager.persist(busyAgent);
        entityManager.flush();
        
        // When
        List<Agent> idleAgents = agentRepository.findByStatus(AgentStatus.IDLE);
        List<Agent> busyAgents = agentRepository.findByStatus(AgentStatus.BUSY);
        
        // Then
        assertThat(idleAgents).hasSize(1);
        assertThat(idleAgents.get(0).getName()).isEqualTo("IdleAgent");
        assertThat(busyAgents).hasSize(1);
        assertThat(busyAgents.get(0).getName()).isEqualTo("BusyAgent");
    }

    @Test
    void findAvailableAgents_CustomQuery_ReturnsIdleAgents() {
        // Given
        Agent idleAgent = new Agent("IdleAgent", AgentType.GENERAL);
        Agent busyAgent = new Agent("BusyAgent", AgentType.GENERAL);
        busyAgent.markBusy();
        
        entityManager.persist(idleAgent);
        entityManager.persist(busyAgent);
        entityManager.flush();
        
        // When
        List<Agent> availableAgents = agentRepository.findAvailableAgents();
        
        // Then
        assertThat(availableAgents).hasSize(1);
        assertThat(availableAgents.get(0).getStatus()).isEqualTo(AgentStatus.IDLE);
    }
}
```

### 2. Service Integration Tests

**AgentServiceIT.java**
```java
@SpringBootTest
@Transactional
class AgentServiceIT {

    @Autowired
    private AgentService agentService;
    
    @Autowired
    private AgentRepository agentRepository;
    
    @Autowired
    private CommandRepository commandRepository;

    @Test
    void createAndExecuteCommand_FullWorkflow_Success() {
        // Given - Create agent
        AgentDto createRequest = new AgentDto("IntegrationTestAgent", AgentType.GENERAL);
        AgentDto createdAgent = agentService.createAgent(createRequest);
        
        // When - Execute command
        CommandRequest commandRequest = new CommandRequest("Hello, test integration");
        CommandResult result = agentService.executeCommand(createdAgent.getId(), commandRequest);
        
        // Then - Verify results
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCommandId()).isNotNull();
        
        // Verify in database
        Agent dbAgent = agentRepository.findById(createdAgent.getId()).orElseThrow();
        assertThat(dbAgent.getStatus()).isEqualTo(AgentStatus.IDLE);
        
        Command dbCommand = commandRepository.findById(result.getCommandId()).orElseThrow();
        assertThat(dbCommand.getStatus()).isEqualTo(CommandStatus.COMPLETED);
        assertThat(dbCommand.getAgent().getId()).isEqualTo(createdAgent.getId());
    }

    @Test
    void getAgentStatistics_WithCommands_ReturnsCorrectStats() {
        // Given - Create agent and execute commands
        AgentDto agent = agentService.createAgent(new AgentDto("StatsAgent", AgentType.GENERAL));
        
        agentService.executeCommand(agent.getId(), new CommandRequest("Command 1"));
        agentService.executeCommand(agent.getId(), new CommandRequest("Command 2"));
        
        // When
        AgentService.AgentStatistics stats = agentService.getAgentStatistics(agent.getId());
        
        // Then
        assertThat(stats.getTotalCommands()).isEqualTo(2);
        assertThat(stats.getSuccessfulCommands()).isEqualTo(2);
        assertThat(stats.getFailedCommands()).isEqualTo(0);
        assertThat(stats.getSuccessRate()).isEqualTo(1.0);
    }
}
```

### 3. Controller Integration Tests

**AgentControllerIT.java**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AgentControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;
    
    @LocalServerPort
    private int port;
    
    private String baseUrl;
    
    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/agents";
    }

    @Test
    void fullAgentLifecycle_CreateExecuteDelete_Success() {
        // 1. Create agent
        AgentDto createRequest = new AgentDto("LifecycleTestAgent", AgentType.GENERAL);
        
        ResponseEntity<ApiResponse> createResponse = restTemplate.postForEntity(
            baseUrl, createRequest, ApiResponse.class);
        
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody().isSuccess()).isTrue();
        
        // Extract agent ID
        Map<String, Object> agentData = (Map<String, Object>) createResponse.getBody().getData();
        Integer agentId = (Integer) agentData.get("id");
        
        // 2. Execute command
        CommandRequest commandRequest = new CommandRequest("Hello from integration test");
        
        ResponseEntity<ApiResponse> executeResponse = restTemplate.postForEntity(
            baseUrl + "/" + agentId + "/execute", commandRequest, ApiResponse.class);
        
        assertThat(executeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(executeResponse.getBody().isSuccess()).isTrue();
        
        // 3. Get agent details
        ResponseEntity<ApiResponse> getResponse = restTemplate.getForEntity(
            baseUrl + "/" + agentId, ApiResponse.class);
        
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> agentDetails = (Map<String, Object>) getResponse.getBody().getData();
        assertThat(agentDetails.get("commandCount")).isEqualTo(1);
        
        // 4. Delete agent
        restTemplate.delete(baseUrl + "/" + agentId);
        
        // 5. Verify deletion
        ResponseEntity<ApiResponse> deletedResponse = restTemplate.getForEntity(
            baseUrl + "/" + agentId, ApiResponse.class);
        
        assertThat(deletedResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
```

## 🌐 End-to-End Testing

**AgentSystemE2ETest.java**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AgentSystemE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;
    
    @LocalServerPort
    private int port;

    @Test
    void multiAgentScenario_CreateMultipleAgentsAndExecuteCommands() {
        String baseUrl = "http://localhost:" + port + "/api/v1/agents";
        
        // Scenario: Create multiple agents and execute different types of commands
        
        // 1. Create different types of agents
        List<String> agentIds = new ArrayList<>();
        
        agentIds.add(createAgent(baseUrl, "GeneralAgent", AgentType.GENERAL));
        agentIds.add(createAgent(baseUrl, "CodeReviewer", AgentType.CODE_REVIEWER));
        agentIds.add(createAgent(baseUrl, "DocumentationAgent", AgentType.DOCUMENTATION));
        
        // 2. Execute different commands on each agent
        executeCommand(baseUrl, agentIds.get(0), "Hello, what can you do?");
        executeCommand(baseUrl, agentIds.get(1), "Please analyze this code: function test() {}");
        executeCommand(baseUrl, agentIds.get(2), "Generate documentation for the API");
        
        // 3. Verify all agents are back to IDLE status
        ResponseEntity<ApiResponse> availableResponse = restTemplate.getForEntity(
            baseUrl + "/available", ApiResponse.class);
        
        assertThat(availableResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> availableAgents = 
            (List<Map<String, Object>>) availableResponse.getBody().getData();
        
        assertThat(availableAgents).hasSize(3);
        
        // 4. Check system health
        ResponseEntity<Map> healthResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health", Map.class);
        
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getBody().get("status")).isEqualTo("UP");
    }
    
    private String createAgent(String baseUrl, String name, AgentType type) {
        AgentDto request = new AgentDto(name, type);
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            baseUrl, request, ApiResponse.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        Map<String, Object> agentData = (Map<String, Object>) response.getBody().getData();
        return agentData.get("id").toString();
    }
    
    private void executeCommand(String baseUrl, String agentId, String command) {
        CommandRequest request = new CommandRequest(command);
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            baseUrl + "/" + agentId + "/execute", request, ApiResponse.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
    }
}
```

## 🏃‍♂️ Running Tests

### Maven Commands

**Run All Tests:**
```bash
./mvnw test
```

**Run Only Unit Tests:**
```bash
./mvnw test -Dtest=**/*Test
```

**Run Only Integration Tests:**
```bash
./mvnw test -Dtest=**/*IT
```

**Run Specific Test Class:**
```bash
./mvnw test -Dtest=AgentServiceTest
```

**Run with Coverage:**
```bash
./mvnw test jacoco:report
```

**Run in Continuous Mode:**
```bash
./mvnw test -Dspring.profiles.active=test --watch
```

### IDE Integration

**IntelliJ IDEA:**
- Right-click test class → "Run Tests"
- Green arrow next to test methods
- Coverage reports with visual indicators

**VS Code:**
- Install "Java Test Runner" extension
- Use Test Explorer panel
- Run/debug individual tests

**Eclipse:**
- Right-click → "Run As" → "JUnit Test"
- Use JUnit view for test management

## 📊 Test Coverage

### Coverage Goals

- **Unit Tests**: 90%+ line coverage
- **Integration Tests**: Cover all critical paths
- **E2E Tests**: Cover main user journeys

### Generating Reports

```bash
# Generate coverage report
./mvnw jacoco:report

# View report
open target/site/jacoco/index.html
```

### Coverage Analysis

**Review Coverage Reports:**
- Line coverage by class
- Branch coverage analysis
- Missed coverage highlights
- Complexity metrics

**Focus Areas:**
- Service layer business logic
- Controller error handling
- Repository custom queries
- Domain model behavior

## 🧪 Test Data Management

### Test Data Builders

**AgentTestDataBuilder.java**
```java
public class AgentTestDataBuilder {
    
    private String name = "TestAgent";
    private AgentType type = AgentType.GENERAL;
    private String description = "Test description";
    private AgentStatus status = AgentStatus.IDLE;
    
    public static AgentTestDataBuilder anAgent() {
        return new AgentTestDataBuilder();
    }
    
    public AgentTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public AgentTestDataBuilder withType(AgentType type) {
        this.type = type;
        return this;
    }
    
    public AgentTestDataBuilder withStatus(AgentStatus status) {
        this.status = status;
        return this;
    }
    
    public Agent build() {
        Agent agent = new Agent(name, type);
        agent.setDescription(description);
        agent.setStatus(status);
        return agent;
    }
    
    public AgentDto buildDto() {
        AgentDto dto = new AgentDto(name, type);
        dto.setDescription(description);
        dto.setStatus(status);
        return dto;
    }
}
```

**Usage Example:**
```java
@Test
void testWithBuilder() {
    // Given
    Agent agent = anAgent()
        .withName("SpecialAgent")
        .withType(AgentType.CODE_REVIEWER)
        .withStatus(AgentStatus.BUSY)
        .build();
    
    // Test logic here...
}
```

### Test Profiles

**application-test.yml**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
  h2:
    console:
      enabled: false

logging:
  level:
    org.springframework.web: WARN
    org.hibernate.SQL: WARN
    com.jamesokeeffe.agentsystem: INFO
```

## 🚨 Test Best Practices

### DO's

✅ **Write Tests First**: TDD approach where possible
✅ **Test Behavior**: Focus on what the code does, not how
✅ **Use Descriptive Names**: Tests should read like specifications
✅ **Arrange-Act-Assert**: Clear test structure
✅ **Test Edge Cases**: Null values, empty collections, boundary conditions
✅ **Mock External Dependencies**: Keep tests isolated and fast
✅ **Use Test Data Builders**: Create readable test data setup

### DON'Ts

❌ **Don't Test Implementation Details**: Test public behavior only
❌ **Don't Write Fragile Tests**: Avoid brittle assertions
❌ **Don't Ignore Failing Tests**: Fix or remove broken tests
❌ **Don't Test Framework Code**: Focus on your business logic
❌ **Don't Use Random Data**: Tests should be predictable
❌ **Don't Share Test State**: Each test should be independent

### Performance Testing

**Load Test Example:**
```java
@Test
@Timeout(value = 5, unit = TimeUnit.SECONDS)
void executeCommand_MultipleSimultaneous_CompletesWithinTimeout() {
    // Test concurrent command execution
    List<CompletableFuture<CommandResult>> futures = IntStream.range(0, 10)
        .mapToObj(i -> CompletableFuture.supplyAsync(() -> 
            agentService.executeCommand(agentId, new CommandRequest("Test " + i))))
        .toList();
    
    List<CommandResult> results = futures.stream()
        .map(CompletableFuture::join)
        .toList();
    
    assertThat(results).hasSize(10);
    assertThat(results).allMatch(CommandResult::isSuccess);
}
```

## 🔍 Debugging Tests

### Common Issues

**Issue: Test Database State**
```java
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
```

**Issue: Async Operations**
```java
@Test
void asyncTest() {
    // Use Awaitility for async testing
    await().atMost(5, SECONDS)
           .untilAsserted(() -> {
               assertThat(result.isComplete()).isTrue();
           });
}
```

**Issue: Time-Dependent Tests**
```java
@Test
void timeTest() {
    // Use Clock abstraction instead of System.currentTimeMillis()
    Clock fixedClock = Clock.fixed(Instant.parse("2024-01-01T10:00:00Z"), ZoneOffset.UTC);
    // Inject clock into service for testing
}
```

---

This comprehensive testing guide ensures high-quality, maintainable code through all layers of the Phase 1 implementation. Each test type serves a specific purpose in the overall quality assurance strategy.