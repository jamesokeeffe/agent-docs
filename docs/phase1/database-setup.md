# Phase 1 Database Setup Guide

This guide covers the database configuration and setup for Phase 1, including H2 embedded database configuration, schema management, and data initialization.

## 🎯 Database Overview

Phase 1 uses H2 as an embedded database for simplicity and ease of development. This setup provides:

- **Zero Configuration**: No external database installation required
- **In-Memory Operation**: Fast performance for development and testing
- **Web Console**: Built-in database management interface
- **Production Ready**: Can be configured for file-based persistence

## 📊 Database Schema

### Entity Relationship Diagram

```
┌─────────────────┐         ┌─────────────────┐
│      Agent      │         │    Command      │
├─────────────────┤         ├─────────────────┤
│ id (PK)         │◄──────┐ │ id (PK)         │
│ name (UNIQUE)   │       │ │ agent_id (FK)   │
│ type            │       │ │ content         │
│ status          │       │ │ context         │
│ configuration   │       │ │ result          │
│ description     │       │ │ status          │
│ created_at      │       │ │ nlp_provider    │
│ updated_at      │       │ │ execution_time  │
└─────────────────┘       │ │ error_message   │
         1                │ │ timestamp       │
         │                │ └─────────────────┘
         │                │
         └────────────────┘
              1:Many
```

### Tables Overview

| Table | Purpose | Relationships |
|-------|---------|---------------|
| `agents` | Store agent definitions and metadata | Parent to commands |
| `commands` | Store command execution history | Child of agents |

## ⚙️ H2 Database Configuration

### Default Configuration

The application is configured to use H2 with the following default settings:

```yaml
# application.yml
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
      settings:
        web-allow-others: false
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        format_sql: true
```

### Configuration Options Explained

**Database URL Parameters:**
- `jdbc:h2:mem:agentdb` - In-memory database named "agentdb"
- `DB_CLOSE_DELAY=-1` - Keep database open until JVM shutdown
- `DB_CLOSE_ON_EXIT=FALSE` - Don't close when last connection closes

**H2 Console Settings:**
- `enabled: true` - Enable web console for database management
- `path: /h2-console` - Console available at this URL path
- `web-allow-others: false` - Security: only local access allowed

**JPA Configuration:**
- `ddl-auto: create-drop` - Recreate schema on startup, drop on shutdown
- `show-sql: true` - Log SQL statements for debugging
- `format_sql: true` - Pretty-print SQL in logs

## 🖥️ H2 Console Access

### Accessing the Console

1. **Start the Application:**
   ```bash
   ./mvnw spring-boot:run
   ```

2. **Open H2 Console:**
   Navigate to: `http://localhost:8080/h2-console`

3. **Connection Settings:**
   ```
   Driver Class: org.h2.Driver
   JDBC URL: jdbc:h2:mem:agentdb
   User Name: sa
   Password: (leave empty)
   ```

4. **Click "Connect"**

### Console Features

**Database Browser:**
- View all tables and their structure
- Browse table data
- View indexes and constraints

**SQL Query Interface:**
- Execute custom SQL queries
- View query results in table format
- Export results to various formats

**Schema Information:**
- View DDL for all tables
- Check foreign key relationships
- Monitor database statistics

### Useful Queries

**View All Agents:**
```sql
SELECT * FROM agents ORDER BY created_at DESC;
```

**View Command History:**
```sql
SELECT c.id, a.name as agent_name, c.content, c.status, c.timestamp 
FROM commands c 
JOIN agents a ON c.agent_id = a.id 
ORDER BY c.timestamp DESC;
```

**Agent Performance Summary:**
```sql
SELECT 
    a.name,
    COUNT(c.id) as total_commands,
    COUNT(CASE WHEN c.status = 'COMPLETED' THEN 1 END) as successful,
    COUNT(CASE WHEN c.status = 'FAILED' THEN 1 END) as failed,
    AVG(c.execution_time_ms) as avg_execution_time
FROM agents a 
LEFT JOIN commands c ON a.id = c.agent_id 
GROUP BY a.id, a.name;
```

## 🗄️ Table Schemas

### Agents Table

```sql
CREATE TABLE agents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    configuration TEXT,
    description VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_agent_name ON agents(name);
CREATE INDEX idx_agent_type_status ON agents(type, status);
```

**Column Details:**

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique identifier |
| `name` | VARCHAR(255) | NOT NULL, UNIQUE | Agent name |
| `type` | VARCHAR(100) | NOT NULL | Agent type enum |
| `status` | VARCHAR(50) | NOT NULL | Current status enum |
| `configuration` | TEXT | NULL | JSON configuration |
| `description` | VARCHAR(1000) | NULL | Human description |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW | Creation timestamp |
| `updated_at` | TIMESTAMP | NOT NULL, AUTO UPDATE | Last update timestamp |

**Valid Enum Values:**

*Agent Types:*
- `GENERAL` - General-purpose agent
- `CODE_REVIEWER` - Code review specialist
- `DOCUMENTATION` - Documentation generator
- `TESTING` - Test creator
- `ORCHESTRATOR` - Workflow coordinator
- `MONITOR` - System monitor

*Agent Statuses:*
- `IDLE` - Available for commands
- `BUSY` - Currently executing
- `OFFLINE` - Not available
- `ERROR` - Error state

### Commands Table

```sql
CREATE TABLE commands (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    context TEXT,
    result TEXT,
    status VARCHAR(50) NOT NULL,
    nlp_provider VARCHAR(100),
    execution_time_ms BIGINT,
    error_message TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_command_agent_id ON commands(agent_id);
CREATE INDEX idx_command_timestamp ON commands(timestamp);
CREATE INDEX idx_command_status ON commands(status);
```

**Column Details:**

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique identifier |
| `agent_id` | BIGINT | NOT NULL, FK to agents | Agent that executed command |
| `content` | TEXT | NOT NULL | Command text |
| `context` | TEXT | NULL | Additional context |
| `result` | TEXT | NULL | Execution result |
| `status` | VARCHAR(50) | NOT NULL | Execution status |
| `nlp_provider` | VARCHAR(100) | NULL | NLP provider used |
| `execution_time_ms` | BIGINT | NULL | Execution time in milliseconds |
| `error_message` | TEXT | NULL | Error details if failed |
| `timestamp` | TIMESTAMP | NOT NULL, DEFAULT NOW | Execution timestamp |

**Valid Status Values:**
- `PENDING` - Waiting to execute
- `EXECUTING` - Currently running
- `COMPLETED` - Successfully finished
- `FAILED` - Execution failed

## 🔄 Schema Management

### Hibernate DDL Auto

The application uses Hibernate's `ddl-auto` feature for schema management:

**Development (create-drop):**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
```
- Creates schema on startup
- Drops schema on shutdown
- **Data is lost** when application stops

**Testing (create-drop):**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
```
- Same as development
- Ensures clean state for each test run

**Production (validate):**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```
- Only validates existing schema
- No automatic changes
- Requires manual schema management

### Manual Schema Creation

For production or persistent development, create the schema manually:

```sql
-- Create database (if using file-based H2)
-- No explicit database creation needed for H2

-- Create tables
CREATE TABLE agents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    configuration TEXT,
    description VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE commands (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    context TEXT,
    result TEXT,
    status VARCHAR(50) NOT NULL,
    nlp_provider VARCHAR(100),
    execution_time_ms BIGINT,
    error_message TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_agent_name ON agents(name);
CREATE INDEX idx_agent_type_status ON agents(type, status);
CREATE INDEX idx_command_agent_id ON commands(agent_id);
CREATE INDEX idx_command_timestamp ON commands(timestamp);
CREATE INDEX idx_command_status ON commands(status);
```

## 🎯 Data Initialization

### Sample Data Script

Create sample data for testing and development:

```sql
-- Insert sample agents
INSERT INTO agents (name, type, status, description) VALUES 
('GeneralAssistant', 'GENERAL', 'IDLE', 'A helpful general-purpose assistant'),
('CodeReviewer', 'CODE_REVIEWER', 'IDLE', 'Specialized in code analysis and review'),
('DocGenerator', 'DOCUMENTATION', 'IDLE', 'Generates and maintains documentation'),
('TestCreator', 'TESTING', 'IDLE', 'Creates and executes tests');

-- Insert sample commands
INSERT INTO commands (agent_id, content, context, result, status, nlp_provider, execution_time_ms) VALUES 
(1, 'Hello, what can you do?', 'Initial greeting', 'Hello! I am ready to help with various tasks.', 'COMPLETED', 'mock', 15),
(1, 'Analyze this function', 'function add(a,b) { return a + b; }', 'This is a simple addition function.', 'COMPLETED', 'mock', 25),
(2, 'Review this code for issues', 'var x = 1; var x = 2;', 'Variable redeclaration detected.', 'COMPLETED', 'mock', 40),
(3, 'Generate docs for API', '/api/v1/agents endpoint', 'Documentation generated for agents endpoint.', 'COMPLETED', 'mock', 100);
```

### Spring Boot Data Initialization

Create a data initialization class:

```java
@Component
public class DataInitializer implements CommandLineRunner {
    
    private final AgentRepository agentRepository;
    
    public DataInitializer(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }
    
    @Override
    public void run(String... args) throws Exception {
        if (agentRepository.count() == 0) {
            initializeSampleData();
        }
    }
    
    private void initializeSampleData() {
        Agent assistant = new Agent("GeneralAssistant", AgentType.GENERAL);
        assistant.setDescription("A helpful general-purpose assistant");
        agentRepository.save(assistant);
        
        Agent reviewer = new Agent("CodeReviewer", AgentType.CODE_REVIEWER);
        reviewer.setDescription("Specialized in code analysis");
        agentRepository.save(reviewer);
    }
}
```

## 🔧 Configuration Variants

### In-Memory Database (Default)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:agentdb
```
- **Pros**: Fast, no file system dependency
- **Cons**: Data lost on restart
- **Use Case**: Development, testing

### File-Based Database

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/agentdb
```
- **Pros**: Data persists across restarts
- **Cons**: Slower than in-memory
- **Use Case**: Local development with data persistence

### TCP Server Mode

```yaml
spring:
  datasource:
    url: jdbc:h2:tcp://localhost/~/agentdb
```
- **Pros**: Multiple applications can connect
- **Cons**: Requires H2 server running
- **Use Case**: Development with multiple instances

### Embedded with Custom Settings

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:agentdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
```
- **MODE=PostgreSQL**: PostgreSQL compatibility mode
- **DATABASE_TO_LOWER=TRUE**: Lowercase identifiers
- **Use Case**: PostgreSQL migration preparation

## 🔍 Monitoring and Maintenance

### Database Statistics

**View Table Sizes:**
```sql
SELECT TABLE_NAME, ROW_COUNT_ESTIMATE 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'PUBLIC';
```

**Index Usage:**
```sql
SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME 
FROM INFORMATION_SCHEMA.INDEXES 
WHERE TABLE_SCHEMA = 'PUBLIC';
```

**Memory Usage:**
```sql
SELECT * FROM INFORMATION_SCHEMA.MEMORY_USAGE;
```

### Performance Monitoring

**Slow Queries:**
Monitor application logs for slow SQL statements when `show-sql: true`.

**Connection Pool:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-test-query: SELECT 1
```

## 🚨 Troubleshooting

### Common Issues

**1. H2 Console Not Accessible**
```
Problem: Cannot access http://localhost:8080/h2-console
Solution: Verify h2.console.enabled=true in configuration
```

**2. Database Connection Failed**
```
Problem: Wrong JDBC URL in H2 console
Solution: Use exact URL from application.yml: jdbc:h2:mem:agentdb
```

**3. Tables Not Found**
```
Problem: Schema not created or wrong database
Solution: Check hibernate.ddl-auto setting and application startup logs
```

**4. Data Lost on Restart**
```
Problem: Using in-memory database
Solution: Switch to file-based URL for persistence
```

### Debug Queries

**Check Schema Exists:**
```sql
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'PUBLIC';
```

**Verify Foreign Keys:**
```sql
SELECT CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME 
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
WHERE REFERENCED_TABLE_NAME IS NOT NULL;
```

**Current Connections:**
```sql
SELECT * FROM INFORMATION_SCHEMA.SESSIONS;
```

## 🚀 Migration to Production Database

### PostgreSQL Migration

When ready for production, switch to PostgreSQL:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/agentdb
    username: agent_user
    password: ${DATABASE_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

**Add PostgreSQL Dependency:**
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Schema Migration Tools

For production schema management, consider:

- **Flyway**: Version-controlled database migrations
- **Liquibase**: Database change management
- **Manual Scripts**: SQL migration scripts

## 📋 Best Practices

### Development
- Use in-memory H2 for fast development cycles
- Enable SQL logging for debugging
- Use H2 console for manual data verification
- Initialize sample data for consistent testing

### Testing
- Use `@Sql` annotations for test data setup
- Use `@DirtiesContext` to ensure clean test state
- Test with both empty and populated databases
- Verify constraint violations are handled

### Production
- Never use H2 in production
- Use connection pooling
- Implement proper backup strategies
- Monitor database performance
- Use schema validation, not auto-generation

---

This database setup guide provides everything needed for Phase 1 development and testing, with clear migration paths for production deployment.