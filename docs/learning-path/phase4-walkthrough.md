# Phase 4 Walkthrough: Complete Local Multi-Agent Copilot System

## Introduction

This walkthrough guides you through implementing Phase 4: the complete local multi-agent Copilot system. You'll integrate all previous phases into a production-ready platform with local LLM support, comprehensive monitoring, and enterprise-grade features.

## Prerequisites

- Completed Phases 1, 2, and 3 walkthroughs
- Understanding of LLM integration patterns
- Knowledge of production deployment practices
- Familiarity with monitoring and observability tools

## Learning Path

### Step 1: Local LLM Integration (90 minutes)

**Objective**: Connect the system with local LLM providers for AI-powered agent capabilities.

1. **Add LLM Dependencies**
   
   Update `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-webflux</artifactId>
   </dependency>
   <dependency>
       <groupId>io.github.ollama4j</groupId>
       <artifactId>ollama4j</artifactId>
       <version>1.0.79</version>
   </dependency>
   <dependency>
       <groupId>dev.ai4j</groupId>
       <artifactId>openai4j</artifactId>
       <version>0.24.0</version>
   </dependency>
   ```

2. **Create LLM Integration Interface**
   
   ```java
   public interface LLMIntegrationService {
       LLMResponse generateCompletion(LLMRequest request);
       LLMResponse generateStreamingCompletion(LLMRequest request, StreamingResponseHandler handler);
       List<EmbeddingVector> generateEmbeddings(List<String> texts, String model);
       List<String> getAvailableModels();
       boolean isHealthy();
   }
   ```

3. **Implement Ollama Integration**
   
   ```java
   @Service
   @Slf4j
   public class OllamaIntegrationService implements LLMIntegrationService {
       
       @Value("${ollama.base-url:http://localhost:11434}")
       private String ollamaBaseUrl;
       
       private OllamaAPI ollamaAPI;
       
       @PostConstruct
       public void initialize() {
           ollamaAPI = new OllamaAPI(ollamaBaseUrl);
           ollamaAPI.setRequestTimeoutSeconds(120);
           
           // Test connection
           try {
               ollamaAPI.listModels();
               log.info("Ollama connection established at {}", ollamaBaseUrl);
           } catch (Exception e) {
               log.warn("Failed to connect to Ollama at {}: {}", ollamaBaseUrl, e.getMessage());
           }
       }
       
       @Override
       public LLMResponse generateCompletion(LLMRequest request) {
           try {
               OllamaResult result = ollamaAPI.generate(
                   request.getModel(),
                   request.getPrompt(),
                   false,
                   new Options()
                       .setTemperature(request.getTemperature())
                       .setTopP(request.getTopP())
                       .setNumPredict(request.getMaxTokens())
               );
               
               return LLMResponse.builder()
                   .content(result.getResponse())
                   .model(request.getModel())
                   .usage(createUsageInfo(result))
                   .responseTime(result.getTotalDuration())
                   .success(true)
                   .build();
               
           } catch (Exception e) {
               log.error("Ollama completion failed", e);
               return LLMResponse.error(e.getMessage());
           }
       }
   }
   ```

4. **Add Model Management**
   
   ```java
   @Component
   public class ModelManager {
       
       private final Map<String, ModelInfo> availableModels = new ConcurrentHashMap<>();
       private final LoadBalancer modelLoadBalancer;
       
       @Scheduled(fixedDelay = 300000) // 5 minutes
       public void refreshAvailableModels() {
           try {
               // Get models from Ollama
               List<Model> ollamaModels = ollamaAPI.listModels();
               
               for (Model model : ollamaModels) {
                   ModelInfo info = ModelInfo.builder()
                       .name(model.getName())
                       .size(model.getSize())
                       .family(model.getDetails().getFamily())
                       .available(true)
                       .lastHealthCheck(LocalDateTime.now())
                       .build();
                   
                   availableModels.put(model.getName(), info);
               }
               
               log.info("Refreshed {} available models", availableModels.size());
               
           } catch (Exception e) {
               log.error("Failed to refresh available models", e);
           }
       }
       
       public String selectOptimalModel(LLMRequest request) {
           return modelLoadBalancer.selectBestModel(
               new ArrayList<>(availableModels.values()), 
               request);
       }
   }
   ```

**Test Your Implementation**:
```bash
# Test Ollama connection
curl -X GET http://localhost:8080/api/v1/llm/models \
  -H "Authorization: Bearer YOUR_TOKEN"

# Generate completion
curl -X POST http://localhost:8080/api/v1/llm/completion \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama2",
    "prompt": "Explain the benefits of multi-agent systems",
    "maxTokens": 200,
    "temperature": 0.7
  }'
```

### Step 2: Production Monitoring Setup (75 minutes)

**Objective**: Implement comprehensive monitoring, metrics, and observability.

1. **Add Monitoring Dependencies**
   
   ```xml
   <dependency>
       <groupId>io.micrometer</groupId>
       <artifactId>micrometer-registry-prometheus</artifactId>
   </dependency>
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-actuator</artifactId>
   </dependency>
   <dependency>
       <groupId>net.logstash.logback</groupId>
       <artifactId>logstash-logback-encoder</artifactId>
       <version>7.4</version>
   </dependency>
   ```

2. **Configure Prometheus Metrics**
   
   ```java
   @Configuration
   @EnablePrometheusEndpoint
   public class MetricsConfiguration {
       
       @Bean
       public MeterRegistry meterRegistry() {
           return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
       }
       
       @Bean
       public AgentSystemMetrics agentSystemMetrics(MeterRegistry meterRegistry) {
           return new AgentSystemMetrics(meterRegistry);
       }
   }
   ```

3. **Implement Custom Metrics**
   
   ```java
   @Component
   public class AgentSystemMetrics {
       
       private final Counter agentRequestCounter;
       private final Timer agentResponseTimer;
       private final Gauge activeAgentsGauge;
       private final Counter llmRequestCounter;
       private final Timer llmResponseTimer;
       
       public AgentSystemMetrics(MeterRegistry meterRegistry) {
           this.agentRequestCounter = Counter.builder("agent_requests_total")
               .description("Total agent requests")
               .register(meterRegistry);
           
           this.agentResponseTimer = Timer.builder("agent_response_duration")
               .description("Agent response time")
               .register(meterRegistry);
           
           this.llmRequestCounter = Counter.builder("llm_requests_total")
               .description("Total LLM requests")
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
       
       public void recordLLMRequest(String model, long duration, boolean success) {
           llmRequestCounter.increment(
               Tags.of(
                   Tag.of("model", model),
                   Tag.of("success", String.valueOf(success))
               )
           );
           
           llmResponseTimer.record(Duration.ofMillis(duration),
               Tags.of(
                   Tag.of("model", model),
                   Tag.of("success", String.valueOf(success))
               )
           );
       }
   }
   ```

4. **Add Health Checks**
   
   ```java
   @Component
   public class SystemHealthIndicator implements HealthIndicator {
       
       @Override
       public Health health() {
           Health.Builder builder = Health.up();
           
           // Check LLM connectivity
           checkLLMHealth(builder);
           
           // Check database
           checkDatabaseHealth(builder);
           
           // Check system resources
           checkSystemResources(builder);
           
           return builder.build();
       }
       
       private void checkLLMHealth(Health.Builder builder) {
           try {
               boolean ollamaHealthy = ollamaService.isHealthy();
               if (ollamaHealthy) {
                   builder.withDetail("ollama.status", "UP");
                   builder.withDetail("ollama.models", ollamaService.getAvailableModels().size());
               } else {
                   builder.down().withDetail("ollama.status", "DOWN");
               }
           } catch (Exception e) {
               builder.down().withDetail("ollama.error", e.getMessage());
           }
       }
   }
   ```

5. **Configure Structured Logging**
   
   Create `logback-spring.xml`:
   ```xml
   <configuration>
       <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
           <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
               <providers>
                   <timestamp/>
                   <logLevel/>
                   <loggerName/>
                   <message/>
                   <mdc/>
                   <stackTrace/>
               </providers>
           </encoder>
       </appender>
       
       <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
           <file>logs/agent-system.log</file>
           <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
               <providers>
                   <timestamp/>
                   <logLevel/>
                   <loggerName/>
                   <message/>
                   <mdc/>
                   <stackTrace/>
               </providers>
           </encoder>
           <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
               <fileNamePattern>logs/agent-system-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
               <maxFileSize>100MB</maxFileSize>
               <maxHistory>30</maxHistory>
           </rollingPolicy>
       </appender>
       
       <root level="INFO">
           <appender-ref ref="STDOUT"/>
           <appender-ref ref="FILE"/>
       </root>
   </configuration>
   ```

**Test Your Implementation**:
```bash
# Check health endpoints
curl http://localhost:8080/actuator/health

# Get Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Check specific metrics
curl http://localhost:8080/actuator/metrics/agent_requests_total
```

### Step 3: Build Management Interface (120 minutes)

**Objective**: Create a comprehensive web interface for system management and interaction.

1. **Add Frontend Dependencies**
   
   Create a React application:
   ```bash
   # In your project root
   npx create-react-app agent-ui --template typescript
   cd agent-ui
   npm install axios recharts react-router-dom @types/node
   ```

2. **Create Dashboard Components**
   
   ```typescript
   // src/components/Dashboard.tsx
   import React, { useState, useEffect } from 'react';
   import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
   
   interface SystemMetrics {
     agentRequests: number;
     llmRequests: number;
     activeAgents: number;
     averageResponseTime: number;
   }
   
   const Dashboard: React.FC = () => {
     const [metrics, setMetrics] = useState<SystemMetrics | null>(null);
     const [performanceData, setPerformanceData] = useState([]);
     
     useEffect(() => {
       const fetchMetrics = async () => {
         try {
           const response = await fetch('/api/v1/metrics/dashboard');
           const data = await response.json();
           setMetrics(data);
         } catch (error) {
           console.error('Failed to fetch metrics:', error);
         }
       };
       
       const fetchPerformanceData = async () => {
         try {
           const response = await fetch('/api/v1/metrics/performance');
           const data = await response.json();
           setPerformanceData(data);
         } catch (error) {
           console.error('Failed to fetch performance data:', error);
         }
       };
       
       fetchMetrics();
       fetchPerformanceData();
       
       // Refresh every 30 seconds
       const interval = setInterval(() => {
         fetchMetrics();
         fetchPerformanceData();
       }, 30000);
       
       return () => clearInterval(interval);
     }, []);
     
     if (!metrics) {
       return <div>Loading...</div>;
     }
     
     return (
       <div className="dashboard">
         <h1>Agent System Dashboard</h1>
         
         <div className="metrics-grid">
           <div className="metric-card">
             <h3>Active Agents</h3>
             <div className="metric-value">{metrics.activeAgents}</div>
           </div>
           
           <div className="metric-card">
             <h3>Total Requests</h3>
             <div className="metric-value">{metrics.agentRequests}</div>
           </div>
           
           <div className="metric-card">
             <h3>LLM Requests</h3>
             <div className="metric-value">{metrics.llmRequests}</div>
           </div>
           
           <div className="metric-card">
             <h3>Avg Response Time</h3>
             <div className="metric-value">{metrics.averageResponseTime}ms</div>
           </div>
         </div>
         
         <div className="performance-chart">
           <h3>Performance Over Time</h3>
           <ResponsiveContainer width="100%" height={300}>
             <LineChart data={performanceData}>
               <CartesianGrid strokeDasharray="3 3" />
               <XAxis dataKey="timestamp" />
               <YAxis />
               <Tooltip />
               <Line type="monotone" dataKey="responseTime" stroke="#8884d8" />
               <Line type="monotone" dataKey="requestCount" stroke="#82ca9d" />
             </LineChart>
           </ResponsiveContainer>
         </div>
       </div>
     );
   };
   
   export default Dashboard;
   ```

3. **Create Agent Management Interface**
   
   ```typescript
   // src/components/AgentManager.tsx
   import React, { useState, useEffect } from 'react';
   
   interface Agent {
     id: number;
     name: string;
     type: string;
     status: string;
     lastActivity: string;
     capabilities: string[];
   }
   
   const AgentManager: React.FC = () => {
     const [agents, setAgents] = useState<Agent[]>([]);
     const [selectedAgent, setSelectedAgent] = useState<Agent | null>(null);
     
     useEffect(() => {
       fetchAgents();
     }, []);
     
     const fetchAgents = async () => {
       try {
         const response = await fetch('/api/v1/agents');
         const data = await response.json();
         setAgents(data);
       } catch (error) {
         console.error('Failed to fetch agents:', error);
       }
     };
     
     const handleAgentCommand = async (agentId: number, command: string) => {
       try {
         const response = await fetch(`/api/v1/agents/${agentId}/command`, {
           method: 'POST',
           headers: {
             'Content-Type': 'application/json',
             'Authorization': `Bearer ${localStorage.getItem('token')}`
           },
           body: JSON.stringify({ command })
         });
         
         const result = await response.json();
         console.log('Command result:', result);
         
         // Refresh agents
         fetchAgents();
       } catch (error) {
         console.error('Failed to send command:', error);
       }
     };
     
     return (
       <div className="agent-manager">
         <h2>Agent Management</h2>
         
         <div className="agent-grid">
           <div className="agent-list">
             <h3>Active Agents</h3>
             {agents.map(agent => (
               <div 
                 key={agent.id} 
                 className={`agent-card ${selectedAgent?.id === agent.id ? 'selected' : ''}`}
                 onClick={() => setSelectedAgent(agent)}
               >
                 <div className="agent-name">{agent.name}</div>
                 <div className="agent-type">{agent.type}</div>
                 <div className={`agent-status ${agent.status.toLowerCase()}`}>
                   {agent.status}
                 </div>
               </div>
             ))}
           </div>
           
           <div className="agent-details">
             {selectedAgent ? (
               <div>
                 <h3>{selectedAgent.name}</h3>
                 <p><strong>Type:</strong> {selectedAgent.type}</p>
                 <p><strong>Status:</strong> {selectedAgent.status}</p>
                 <p><strong>Last Activity:</strong> {selectedAgent.lastActivity}</p>
                 
                 <div className="capabilities">
                   <h4>Capabilities:</h4>
                   <ul>
                     {selectedAgent.capabilities.map(capability => (
                       <li key={capability}>{capability}</li>
                     ))}
                   </ul>
                 </div>
                 
                 <div className="agent-actions">
                   <button onClick={() => handleAgentCommand(selectedAgent.id, 'status')}>
                     Get Status
                   </button>
                   <button onClick={() => handleAgentCommand(selectedAgent.id, 'restart')}>
                     Restart
                   </button>
                   <button onClick={() => handleAgentCommand(selectedAgent.id, 'shutdown')}>
                     Shutdown
                   </button>
                 </div>
               </div>
             ) : (
               <div>Select an agent to view details</div>
             )}
           </div>
         </div>
       </div>
     );
   };
   
   export default AgentManager;
   ```

4. **Create Workflow Visualization**
   
   ```typescript
   // src/components/WorkflowVisualizer.tsx
   import React, { useState, useEffect } from 'react';
   
   interface WorkflowExecution {
     id: string;
     workflowId: string;
     status: string;
     progress: number;
     steps: WorkflowStep[];
     startTime: string;
     endTime?: string;
   }
   
   interface WorkflowStep {
     id: string;
     name: string;
     status: string;
     startTime?: string;
     endTime?: string;
     dependencies: string[];
   }
   
   const WorkflowVisualizer: React.FC = () => {
     const [executions, setExecutions] = useState<WorkflowExecution[]>([]);
     const [selectedExecution, setSelectedExecution] = useState<WorkflowExecution | null>(null);
     
     useEffect(() => {
       fetchExecutions();
       
       // Poll for updates
       const interval = setInterval(fetchExecutions, 5000);
       return () => clearInterval(interval);
     }, []);
     
     const fetchExecutions = async () => {
       try {
         const response = await fetch('/api/v1/workflows/executions');
         const data = await response.json();
         setExecutions(data);
       } catch (error) {
         console.error('Failed to fetch executions:', error);
       }
     };
     
     const renderWorkflowDiagram = (execution: WorkflowExecution) => {
       return (
         <div className="workflow-diagram">
           <h4>Workflow Steps</h4>
           <div className="steps-container">
             {execution.steps.map(step => (
               <div key={step.id} className={`step ${step.status.toLowerCase()}`}>
                 <div className="step-name">{step.name}</div>
                 <div className="step-status">{step.status}</div>
                 {step.dependencies.length > 0 && (
                   <div className="step-dependencies">
                     Depends on: {step.dependencies.join(', ')}
                   </div>
                 )}
               </div>
             ))}
           </div>
         </div>
       );
     };
     
     return (
       <div className="workflow-visualizer">
         <h2>Workflow Executions</h2>
         
         <div className="execution-grid">
           <div className="execution-list">
             {executions.map(execution => (
               <div 
                 key={execution.id} 
                 className={`execution-card ${execution.status.toLowerCase()}`}
                 onClick={() => setSelectedExecution(execution)}
               >
                 <div className="execution-id">{execution.id}</div>
                 <div className="execution-workflow">{execution.workflowId}</div>
                 <div className="execution-status">{execution.status}</div>
                 <div className="execution-progress">
                   <div className="progress-bar">
                     <div 
                       className="progress-fill" 
                       style={{ width: `${execution.progress}%` }}
                     ></div>
                   </div>
                   <span>{execution.progress}%</span>
                 </div>
               </div>
             ))}
           </div>
           
           <div className="execution-details">
             {selectedExecution ? (
               <div>
                 <h3>Execution Details</h3>
                 <p><strong>ID:</strong> {selectedExecution.id}</p>
                 <p><strong>Workflow:</strong> {selectedExecution.workflowId}</p>
                 <p><strong>Status:</strong> {selectedExecution.status}</p>
                 <p><strong>Progress:</strong> {selectedExecution.progress}%</p>
                 <p><strong>Started:</strong> {selectedExecution.startTime}</p>
                 {selectedExecution.endTime && (
                   <p><strong>Completed:</strong> {selectedExecution.endTime}</p>
                 )}
                 
                 {renderWorkflowDiagram(selectedExecution)}
               </div>
             ) : (
               <div>Select an execution to view details</div>
             )}
           </div>
         </div>
       </div>
     );
   };
   
   export default WorkflowVisualizer;
   ```

**Test Your Implementation**:
```bash
# Build and serve the React app
cd agent-ui
npm run build
cd ..

# Copy build files to Spring Boot static resources
cp -r agent-ui/build/* src/main/resources/static/

# Access the UI
open http://localhost:8080
```

### Step 4: Production Deployment (60 minutes)

**Objective**: Prepare the system for production deployment with Docker and monitoring.

1. **Create Dockerfile**
   
   ```dockerfile
   # Multi-stage build
   FROM node:18-alpine AS frontend-build
   WORKDIR /app
   COPY agent-ui/package*.json ./
   RUN npm ci --only=production
   COPY agent-ui/ ./
   RUN npm run build
   
   FROM openjdk:17-jdk-slim AS backend-build
   WORKDIR /app
   COPY pom.xml ./
   COPY src ./src
   COPY --from=frontend-build /app/build ./src/main/resources/static
   RUN ./mvnw clean package -DskipTests
   
   FROM openjdk:17-jre-slim
   WORKDIR /app
   
   # Install curl for health checks
   RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
   
   # Create non-root user
   RUN groupadd -r agentuser && useradd -r -g agentuser agentuser
   
   # Copy application
   COPY --from=backend-build /app/target/agent-docs-*.jar app.jar
   
   # Set ownership
   RUN chown -R agentuser:agentuser /app
   USER agentuser
   
   # Health check
   HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
     CMD curl -f http://localhost:8080/actuator/health || exit 1
   
   EXPOSE 8080
   
   ENTRYPOINT ["java", "-jar", "app.jar"]
   ```

2. **Create Docker Compose**
   
   ```yaml
   # docker-compose.yml
   version: '3.8'
   
   services:
     agent-system:
       build: .
       ports:
         - "8080:8080"
       environment:
         - SPRING_PROFILES_ACTIVE=docker
         - OLLAMA_BASE_URL=http://ollama:11434
         - SPRING_DATASOURCE_URL=jdbc:h2:file:/data/agentdb
       volumes:
         - agent-data:/data
         - ./logs:/app/logs
       depends_on:
         - ollama
         - prometheus
       restart: unless-stopped
       healthcheck:
         test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
         interval: 30s
         timeout: 10s
         retries: 3
         start_period: 60s
   
     ollama:
       image: ollama/ollama:latest
       ports:
         - "11434:11434"
       volumes:
         - ollama-data:/root/.ollama
       restart: unless-stopped
       environment:
         - OLLAMA_NUM_PARALLEL=2
         - OLLAMA_MAX_LOADED_MODELS=2
   
     prometheus:
       image: prom/prometheus:latest
       ports:
         - "9090:9090"
       volumes:
         - ./prometheus.yml:/etc/prometheus/prometheus.yml
         - prometheus-data:/prometheus
       command:
         - '--config.file=/etc/prometheus/prometheus.yml'
         - '--storage.tsdb.path=/prometheus'
         - '--web.console.libraries=/usr/share/prometheus/console_libraries'
         - '--web.console.templates=/usr/share/prometheus/consoles'
         - '--web.enable-lifecycle'
       restart: unless-stopped
   
     grafana:
       image: grafana/grafana:latest
       ports:
         - "3000:3000"
       volumes:
         - grafana-data:/var/lib/grafana
         - ./grafana/dashboards:/etc/grafana/provisioning/dashboards
         - ./grafana/datasources:/etc/grafana/provisioning/datasources
       environment:
         - GF_SECURITY_ADMIN_PASSWORD=admin123
       restart: unless-stopped
   
   volumes:
     agent-data:
     ollama-data:
     prometheus-data:
     grafana-data:
   ```

3. **Create Prometheus Configuration**
   
   ```yaml
   # prometheus.yml
   global:
     scrape_interval: 15s
     evaluation_interval: 15s
   
   scrape_configs:
     - job_name: 'agent-system'
       static_configs:
         - targets: ['agent-system:8080']
       metrics_path: '/actuator/prometheus'
       scrape_interval: 10s
   
     - job_name: 'prometheus'
       static_configs:
         - targets: ['localhost:9090']
   ```

4. **Create Grafana Dashboard**
   
   ```json
   {
     "dashboard": {
       "id": null,
       "title": "Agent System Dashboard",
       "tags": ["agent-system"],
       "timezone": "browser",
       "panels": [
         {
           "id": 1,
           "title": "Agent Requests",
           "type": "graph",
           "targets": [
             {
               "expr": "rate(agent_requests_total[5m])",
               "legendFormat": "{{agent_type}} - {{operation}}"
             }
           ],
           "yAxes": [
             {
               "label": "Requests/sec"
             }
           ]
         },
         {
           "id": 2,
           "title": "Response Times",
           "type": "graph",
           "targets": [
             {
               "expr": "histogram_quantile(0.95, rate(agent_response_duration_bucket[5m]))",
               "legendFormat": "95th percentile"
             },
             {
               "expr": "histogram_quantile(0.50, rate(agent_response_duration_bucket[5m]))",
               "legendFormat": "50th percentile"
             }
           ]
         },
         {
           "id": 3,
           "title": "LLM Requests",
           "type": "stat",
           "targets": [
             {
               "expr": "sum(rate(llm_requests_total[5m]))",
               "legendFormat": "LLM Requests/sec"
             }
           ]
         }
       ],
       "time": {
         "from": "now-1h",
         "to": "now"
       },
       "refresh": "5s"
     }
   }
   ```

**Test Your Implementation**:
```bash
# Build and start the system
docker-compose up --build

# Test the complete system
curl http://localhost:8080/actuator/health

# Access Grafana dashboard
open http://localhost:3000
# Login: admin/admin123

# Test agent with LLM
curl -X POST http://localhost:8080/api/v1/agents/1/command \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "command": "analyze this code for security issues",
    "context": {
      "code": "public class UserController { @GetMapping public String getUser(@RequestParam String id) { return \"SELECT * FROM users WHERE id = \" + id; } }"
    }
  }'
```

### Step 5: Performance Optimization (45 minutes)

**Objective**: Optimize the system for production-grade performance.

1. **Connection Pooling**
   
   ```yaml
   # application-docker.yml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20
         minimum-idle: 5
         connection-timeout: 30000
         idle-timeout: 600000
         max-lifetime: 1800000
   ```

2. **Caching Configuration**
   
   ```java
   @Configuration
   @EnableCaching
   public class CacheConfiguration {
       
       @Bean
       public CacheManager cacheManager() {
           CaffeineCacheManager cacheManager = new CaffeineCacheManager();
           cacheManager.setCaffeine(Caffeine.newBuilder()
               .maximumSize(1000)
               .expireAfterWrite(Duration.ofMinutes(10))
               .recordStats());
           return cacheManager;
       }
       
       @Bean
       public Cache<String, LLMResponse> llmResponseCache() {
           return Caffeine.newBuilder()
               .maximumSize(500)
               .expireAfterWrite(Duration.ofMinutes(5))
               .build();
       }
   }
   ```

3. **Async Configuration**
   
   ```java
   @Configuration
   @EnableAsync
   public class AsyncConfiguration {
       
       @Bean(name = "taskExecutor")
       public TaskExecutor taskExecutor() {
           ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
           executor.setCorePoolSize(10);
           executor.setMaxPoolSize(50);
           executor.setQueueCapacity(100);
           executor.setThreadNamePrefix("agent-task-");
           executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
           executor.initialize();
           return executor;
       }
       
       @Bean(name = "llmExecutor")
       public TaskExecutor llmExecutor() {
           ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
           executor.setCorePoolSize(5);
           executor.setMaxPoolSize(20);
           executor.setQueueCapacity(50);
           executor.setThreadNamePrefix("llm-task-");
           executor.initialize();
           return executor;
       }
   }
   ```

## Hands-On Exercises

### Exercise 1: Custom Code Intelligence
**Time**: 60 minutes

Build a complete code intelligence pipeline:

```java
@Component
public class CodeIntelligenceService {
    
    @Async("taskExecutor")
    public CompletableFuture<CodeAnalysisResult> analyzeCode(String code, String language) {
        return CompletableFuture.supplyAsync(() -> {
            // 1. Syntax analysis
            SyntaxAnalysisResult syntax = syntaxAnalyzer.analyze(code, language);
            
            // 2. Security scan
            SecurityScanResult security = securityScanner.scan(code);
            
            // 3. Quality metrics
            QualityMetrics quality = qualityAnalyzer.analyze(code);
            
            // 4. Generate suggestions with LLM
            LLMRequest llmRequest = LLMRequest.builder()
                .prompt(createCodeAnalysisPrompt(code, syntax, security, quality))
                .model("codellama")
                .maxTokens(500)
                .build();
            
            LLMResponse llmResponse = llmService.generateCompletion(llmRequest);
            
            return CodeAnalysisResult.builder()
                .syntaxAnalysis(syntax)
                .securityScan(security)
                .qualityMetrics(quality)
                .suggestions(llmResponse.getContent())
                .build();
        });
    }
}
```

### Exercise 2: Load Testing
**Time**: 30 minutes

Create load tests for the complete system:

```java
@Test
void shouldHandleHighConcurrentLoad() {
    int numberOfRequests = 100;
    int numberOfThreads = 10;
    
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
    
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < numberOfRequests; i++) {
        final int requestNumber = i;
        Future<ResponseEntity<String>> future = executor.submit(() -> {
            return restTemplate.postForEntity(
                "/api/v1/agents/1/command",
                createTestRequest(requestNumber),
                String.class
            );
        });
        futures.add(future);
    }
    
    // Wait for all requests to complete
    int successCount = 0;
    for (Future<ResponseEntity<String>> future : futures) {
        try {
            ResponseEntity<String> response = future.get(30, TimeUnit.SECONDS);
            if (response.getStatusCode().is2xxSuccessful()) {
                successCount++;
            }
        } catch (Exception e) {
            log.error("Request failed", e);
        }
    }
    
    long totalTime = System.currentTimeMillis() - startTime;
    double requestsPerSecond = (double) numberOfRequests / (totalTime / 1000.0);
    
    log.info("Load test results: {}/{} successful, {} requests/sec", 
        successCount, numberOfRequests, requestsPerSecond);
    
    assertThat(successCount).isGreaterThan(numberOfRequests * 0.95); // 95% success rate
    assertThat(requestsPerSecond).isGreaterThan(10); // Minimum 10 requests/sec
    
    executor.shutdown();
}
```

### Exercise 3: End-to-End Workflow
**Time**: 45 minutes

Create a complete CI/CD workflow using all system components:

```json
{
  "id": "full-ci-cd-pipeline",
  "name": "Complete CI/CD Pipeline with AI Analysis",
  "description": "End-to-end pipeline with AI-powered code analysis",
  "steps": [
    {
      "id": "checkout",
      "name": "Checkout Code",
      "type": "mcp-tool",
      "tool": "git",
      "parameters": {
        "operation": "checkout",
        "repository": "${repository}",
        "branch": "${branch}"
      }
    },
    {
      "id": "ai-code-review",
      "name": "AI Code Review",
      "type": "agent-task",
      "agent": "code-reviewer",
      "parameters": {
        "analysisType": "comprehensive",
        "includeSecurityScan": true,
        "generateSuggestions": true
      },
      "dependencies": ["checkout"]
    },
    {
      "id": "generate-tests",
      "name": "AI Test Generation",
      "type": "agent-task",
      "agent": "test-generator",
      "parameters": {
        "testType": "unit",
        "coverage": "branch",
        "frameworks": ["junit", "mockito"]
      },
      "dependencies": ["ai-code-review"]
    },
    {
      "id": "run-tests",
      "name": "Execute Tests",
      "type": "mcp-tool",
      "tool": "docker",
      "parameters": {
        "operation": "run",
        "image": "maven:3.8",
        "command": "mvn test"
      },
      "dependencies": ["generate-tests"]
    },
    {
      "id": "build-image",
      "name": "Build Docker Image",
      "type": "mcp-tool",
      "tool": "docker",
      "parameters": {
        "operation": "build",
        "tag": "${repository}:${version}",
        "dockerfile": "Dockerfile"
      },
      "dependencies": ["run-tests"]
    },
    {
      "id": "deploy",
      "name": "Deploy to Environment",
      "type": "agent-task",
      "agent": "deployment-manager",
      "parameters": {
        "environment": "${environment}",
        "image": "${repository}:${version}",
        "healthCheckUrl": "/actuator/health"
      },
      "dependencies": ["build-image"],
      "conditions": [
        {"type": "approval", "required": true},
        {"type": "branch", "value": "main"}
      ]
    }
  ],
  "configuration": {
    "maxConcurrentSteps": 3,
    "globalTimeout": 1800,
    "errorHandling": "fail-fast",
    "notifications": {
      "onComplete": true,
      "onError": true,
      "webhook": "${webhook_url}"
    }
  }
}
```

## What You've Built

After completing Phase 4, you now have:

1. **Complete Local Copilot System**: Fully integrated multi-agent platform
2. **Local LLM Integration**: Support for Ollama, LocalAI, and custom endpoints
3. **Production Monitoring**: Comprehensive observability with Prometheus and Grafana
4. **Management Interface**: Full-featured web UI for system administration
5. **Production Deployment**: Docker containers and orchestration setup
6. **Performance Optimization**: Caching, connection pooling, and async processing
7. **End-to-End Workflows**: Complete CI/CD pipelines with AI integration

## Production Readiness Checklist

- [x] **Security**: JWT authentication, role-based access, input validation
- [x] **Monitoring**: Prometheus metrics, health checks, structured logging
- [x] **Performance**: Connection pooling, caching, async processing
- [x] **Scalability**: Load balancing, resource management, horizontal scaling support
- [x] **Reliability**: Circuit breakers, retry mechanisms, error handling
- [x] **Deployment**: Docker containers, compose files, environment configuration
- [x] **Documentation**: API docs, deployment guides, operational runbooks
- [x] **Testing**: Unit tests, integration tests, load tests, end-to-end tests

## Next Steps

Congratulations! You've built a complete, production-ready multi-agent system. Consider these extensions:

1. **Custom Domain Integration**: Extend for your specific use case
2. **Advanced AI Features**: Add more sophisticated LLM capabilities
3. **Enterprise Features**: SSO integration, advanced security, compliance
4. **Cloud Deployment**: Kubernetes manifests, cloud-native features
5. **API Ecosystem**: Build marketplace for custom plugins and workflows
6. **Community Features**: Open source contributions, documentation, tutorials

You now have a solid foundation for building sophisticated multi-agent applications suitable for enterprise deployment and further innovation.