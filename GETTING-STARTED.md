# Getting Started with Multi-Agent System

Welcome to the comprehensive guide for building your own local multi-agent system! This guide will walk you through setting up and running the system from scratch.

## 📋 Prerequisites

### Required Software
- **Java 17+** - [Download OpenJDK](https://adoptium.net/)
- **Maven 3.6+** - [Installation Guide](https://maven.apache.org/install.html)
- **Git** - [Download Git](https://git-scm.com/downloads)

### Optional (for advanced features)
- **Docker** - [Get Docker](https://docs.docker.com/get-docker/)
- **PostgreSQL** - [Installation Guide](https://www.postgresql.org/download/)
- **Redis** - [Quick Start](https://redis.io/docs/getting-started/)

### Verify Installation
```bash
# Check Java version
java -version

# Check Maven version
mvn -version

# Check Git version
git --version
```

## 🚀 Quick Setup (5 Minutes)

### 1. Clone the Repository
```bash
git clone https://github.com/jamesokeeffe/agent-docs.git
cd agent-docs
```

### 2. Build the Project
```bash
# Download dependencies and build
./mvnw clean install

# Skip tests for faster initial build
./mvnw clean install -DskipTests
```

### 3. Run the Application
```bash
# Start the Spring Boot application
./mvnw spring-boot:run
```

### 4. Verify It's Working
```bash
# Test the health endpoint
curl http://localhost:8080/actuator/health

# Test a simple agent command
curl -X POST http://localhost:8080/api/agents/execute \
  -H "Content-Type: application/json" \
  -d '{"command": "Hello, what can you do?", "context": "Getting started"}'
```

## 📖 Learning Path

### Phase 1: Foundation (Start Here)
Perfect for beginners who want to understand agent fundamentals.

```bash
# Follow the Phase 1 tutorial
open docs/learning-path/phase1-walkthrough.md

# Or view in browser
http://localhost:8080/docs/phase1/README.md
```

**What you'll build:**
- Basic autonomous agent with REST API
- Natural language command processing
- Simple memory and persistence
- Health monitoring

**Time commitment:** 2-3 hours

### Phase 2: Enhanced Capabilities
Add AI integration and plugin system.

```bash
# Prerequisites: Complete Phase 1
open docs/learning-path/phase2-walkthrough.md
```

**What you'll add:**
- OpenAI/Anthropic API integration
- Plugin system architecture
- JWT authentication
- Inter-agent communication

**Time commitment:** 3-4 hours

### Phase 3: Multi-Agent Orchestration
Build a coordinated agent ecosystem.

```bash
# Prerequisites: Complete Phases 1-2
open docs/learning-path/phase3-walkthrough.md
```

**What you'll build:**
- Agent registry and discovery
- Workflow orchestration
- Event-driven architecture
- Load balancing

**Time commitment:** 4-5 hours

### Phase 4: Production Ready
MCP integration and enterprise features.

```bash
# Prerequisites: Complete Phases 1-3
open docs/learning-path/phase4-walkthrough.md
```

**What you'll implement:**
- Model Context Protocol (MCP)
- Production deployment
- Advanced monitoring
- Kubernetes manifests

**Time commitment:** 5-6 hours

## 🛠️ Development Environment Setup

### IDE Configuration

#### IntelliJ IDEA
1. Import as Maven project
2. Install Spring Boot plugin
3. Configure Java 17 SDK
4. Enable annotation processing

#### VS Code
1. Install Java Extension Pack
2. Install Spring Boot Extension Pack
3. Configure Java path in settings

#### Eclipse
1. Import existing Maven project
2. Install Spring Tools 4
3. Configure Java Build Path

### Database Setup

#### H2 Database (Default)
No setup required! The H2 database runs embedded and creates tables automatically.

**Access H2 Console:**
```bash
# Start application and visit
http://localhost:8080/h2-console

# Default connection settings:
JDBC URL: jdbc:h2:mem:agentdb
Username: sa
Password: (leave empty)
```

#### PostgreSQL (Production)
```bash
# Install PostgreSQL
brew install postgresql          # macOS
sudo apt install postgresql     # Ubuntu
choco install postgresql        # Windows

# Create database
createdb agentdb

# Update application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/agentdb
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### Environment Variables
Create a `.env` file in the project root:

```bash
# Database Configuration
DATABASE_URL=jdbc:h2:mem:agentdb
DATABASE_USERNAME=sa
DATABASE_PASSWORD=

# API Keys (for Phase 2+)
OPENAI_API_KEY=your_openai_key_here
ANTHROPIC_API_KEY=your_anthropic_key_here

# Security (for Phase 2+)
JWT_SECRET=your_jwt_secret_here

# Redis (for Phase 2+)
REDIS_URL=redis://localhost:6379
```

## 🧪 Running Tests

### All Tests
```bash
./mvnw test
```

### Specific Test Categories
```bash
# Unit tests only
./mvnw test -Dtest=**/*UnitTest

# Integration tests only
./mvnw test -Dtest=**/*IntegrationTest

# Phase-specific tests
./mvnw test -Dtest=**/*Phase1*Test
```

### Test Coverage
```bash
# Generate coverage report
./mvnw jacoco:report

# View report
open target/site/jacoco/index.html
```

## 🐛 Troubleshooting

### Common Issues

#### Port 8080 Already in Use
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>

# Or use different port
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

#### Java Version Issues
```bash
# Check Java version
java -version

# Set JAVA_HOME (macOS/Linux)
export JAVA_HOME=/path/to/java17

# Set JAVA_HOME (Windows)
set JAVA_HOME=C:\path\to\java17
```

#### Maven Build Failures
```bash
# Clear Maven cache
./mvnw dependency:purge-local-repository

# Rebuild from scratch
./mvnw clean install -U
```

#### Database Connection Issues
```bash
# Check if H2 is accessible
curl http://localhost:8080/h2-console

# Verify database configuration
./mvnw spring-boot:run --debug
```

### Getting Help

1. **Check the logs**: Look at console output for error details
2. **Review documentation**: Each phase has detailed troubleshooting guides
3. **Search issues**: Check GitHub issues for known problems
4. **Create an issue**: Report new bugs with full error details

## 🔧 Configuration Options

### Application Properties
Key configuration options in `src/main/resources/application.yml`:

```yaml
# Server Configuration
server:
  port: 8080
  servlet:
    context-path: /

# Agent Configuration
agent:
  max-memory-size: 1000
  command-timeout: 30s
  nlp-provider: "mock"  # or "openai", "anthropic"

# Database Configuration
spring:
  datasource:
    url: jdbc:h2:mem:agentdb
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

# Logging Configuration
logging:
  level:
    com.jamesokeeffe.agentsystem: DEBUG
    org.springframework.web: INFO
```

### Profile-Based Configuration

#### Development Profile
```yaml
# application-dev.yml
agent:
  nlp-provider: "mock"
  debug-mode: true
logging:
  level:
    root: DEBUG
```

#### Production Profile
```yaml
# application-prod.yml
agent:
  nlp-provider: "openai"
  debug-mode: false
logging:
  level:
    root: INFO
```

Run with specific profile:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## 📚 Next Steps

### Immediate Next Steps
1. ✅ Complete the quick setup above
2. 📖 Read [System Architecture](ARCHITECTURE.md)
3. 🏗️ Start [Phase 1 Tutorial](docs/learning-path/phase1-walkthrough.md)
4. 🧪 Run your first tests

### Learning Resources
- [📚 Complete Learning Path](docs/learning-path/README.md)
- [🏗️ Architecture Deep Dive](ARCHITECTURE.md)
- [📖 API Reference](API-REFERENCE.md)
- [💡 Best Practices](docs/guides/coding-standards.md)

### Community
- **Issues**: Report bugs and request features
- **Discussions**: Ask questions and share ideas
- **Contributing**: See [CONTRIBUTING.md](CONTRIBUTING.md)

---

**Ready to start building?** Head to the [Phase 1 Tutorial](docs/learning-path/phase1-walkthrough.md) and build your first agent! 🚀