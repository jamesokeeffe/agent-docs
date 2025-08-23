# Phase 1 API Guide - REST Endpoints Documentation

This comprehensive guide covers all REST API endpoints available in Phase 1 of the multi-agent system, including request/response formats, examples, and best practices.

## 📋 API Overview

### Base URL
```
http://localhost:8080/api/v1
```

### Response Format
All endpoints return responses in a consistent format:

```json
{
  "success": true|false,
  "data": <response_data>,
  "message": "descriptive_message",
  "error": {
    "code": "ERROR_CODE",
    "message": "error_description",
    "details": {...}
  },
  "timestamp": "2024-01-01T00:00:00Z",
  "version": "1.0.0"
}
```

### HTTP Status Codes
- `200 OK` - Successful operation
- `201 Created` - Resource created successfully
- `204 No Content` - Successful deletion
- `400 Bad Request` - Invalid request data
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource conflict (e.g., duplicate name)
- `422 Unprocessable Entity` - Command execution failed
- `500 Internal Server Error` - Server error

## 🤖 Agent Management Endpoints

### 1. Create Agent

Creates a new agent in the system.

**Endpoint:** `POST /agents`

**Request Body:**
```json
{
  "name": "string (required, 1-255 chars, unique)",
  "type": "GENERAL|CODE_REVIEWER|DOCUMENTATION|TESTING|ORCHESTRATOR|MONITOR",
  "description": "string (optional, max 1000 chars)",
  "configuration": "string (optional, JSON format)"
}
```

**Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Assistant",
    "type": "GENERAL",
    "status": "IDLE",
    "description": "A helpful general-purpose assistant",
    "configuration": null,
    "commandCount": 0,
    "createdAt": "2024-01-01T10:00:00Z",
    "updatedAt": "2024-01-01T10:00:00Z"
  },
  "message": "Agent created successfully",
  "timestamp": "2024-01-01T10:00:00Z",
  "version": "1.0.0"
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/agents \
  -H "Content-Type: application/json" \
  -d '{
    "name": "CodeHelper",
    "type": "GENERAL",
    "description": "An agent that helps with code-related tasks"
  }'
```

**Error Responses:**
```json
// Duplicate name
{
  "success": false,
  "error": {
    "code": "AGENT_CREATION_FAILED",
    "message": "Agent with name 'Assistant' already exists"
  }
}

// Validation error
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Input validation failed",
    "details": {
      "name": "Agent name is required"
    }
  }
}
```

### 2. List Agents

Retrieves all agents with pagination support.

**Endpoint:** `GET /agents`

**Query Parameters:**
- `page` (optional): Page number (0-based, default: 0)
- `size` (optional): Page size (default: 20, max: 100)
- `sort` (optional): Sort criteria (e.g., `name,asc` or `createdAt,desc`)

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Assistant",
        "type": "GENERAL",
        "status": "IDLE",
        "description": "A helpful assistant",
        "commandCount": 5,
        "createdAt": "2024-01-01T10:00:00Z",
        "updatedAt": "2024-01-01T10:30:00Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": {
        "empty": true,
        "sorted": false,
        "unsorted": true
      }
    },
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true,
    "numberOfElements": 1,
    "empty": false
  },
  "message": "Agents retrieved successfully"
}
```

**Examples:**
```bash
# Get first page with default size
curl http://localhost:8080/api/v1/agents

# Get specific page with custom size
curl "http://localhost:8080/api/v1/agents?page=1&size=10"

# Sort by name ascending
curl "http://localhost:8080/api/v1/agents?sort=name,asc"

# Sort by creation date descending
curl "http://localhost:8080/api/v1/agents?sort=createdAt,desc"
```

### 3. Get Agent by ID

Retrieves a specific agent by its ID.

**Endpoint:** `GET /agents/{id}`

**Path Parameters:**
- `id`: Agent ID (required)

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Assistant",
    "type": "GENERAL",
    "status": "IDLE",
    "description": "A helpful assistant",
    "configuration": "{\"maxMemory\": 1000}",
    "commandCount": 5,
    "createdAt": "2024-01-01T10:00:00Z",
    "updatedAt": "2024-01-01T10:30:00Z"
  },
  "message": "Agent retrieved successfully"
}
```

**Example:**
```bash
curl http://localhost:8080/api/v1/agents/1
```

**Error Response:** `404 Not Found`
```json
{
  "success": false,
  "error": {
    "code": "AGENT_NOT_FOUND",
    "message": "Agent with ID 999 not found"
  }
}
```

### 4. Update Agent

Updates an existing agent's information.

**Endpoint:** `PUT /agents/{id}`

**Path Parameters:**
- `id`: Agent ID (required)

**Request Body:**
```json
{
  "name": "string (required, 1-255 chars)",
  "type": "GENERAL|CODE_REVIEWER|DOCUMENTATION|TESTING|ORCHESTRATOR|MONITOR",
  "description": "string (optional, max 1000 chars)",
  "configuration": "string (optional, JSON format)"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "UpdatedAssistant",
    "type": "GENERAL",
    "status": "IDLE",
    "description": "An updated helpful assistant",
    "configuration": "{\"maxMemory\": 2000}",
    "commandCount": 5,
    "createdAt": "2024-01-01T10:00:00Z",
    "updatedAt": "2024-01-01T11:00:00Z"
  },
  "message": "Agent updated successfully"
}
```

**Example:**
```bash
curl -X PUT http://localhost:8080/api/v1/agents/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ImprovedAssistant",
    "type": "GENERAL",
    "description": "An improved version of the assistant"
  }'
```

### 5. Delete Agent

Deletes an agent from the system.

**Endpoint:** `DELETE /agents/{id}`

**Path Parameters:**
- `id`: Agent ID (required)

**Response:** `204 No Content`

**Example:**
```bash
curl -X DELETE http://localhost:8080/api/v1/agents/1
```

**Error Responses:**
```json
// Agent not found
HTTP 404 Not Found

// Agent is busy
{
  "success": false,
  "error": {
    "code": "AGENT_DELETE_FAILED",
    "message": "Cannot delete busy agent. Wait for current command to complete."
  }
}
```

## ⚡ Command Execution Endpoints

### 1. Execute Command

Executes a natural language command on a specific agent.

**Endpoint:** `POST /agents/{id}/execute`

**Path Parameters:**
- `id`: Agent ID (required)

**Request Body:**
```json
{
  "command": "string (required, 1-5000 chars)",
  "context": "string (optional, max 2000 chars)",
  "nlpProvider": "string (optional, default: 'mock')"
}
```

**Response:** `200 OK` (success) or `422 Unprocessable Entity` (execution failed)
```json
{
  "success": true,
  "data": {
    "commandId": 1,
    "status": "COMPLETED",
    "result": "Hello! I'm your agent assistant. I'm ready to help you with various tasks.",
    "errorMessage": null,
    "executionTimeMs": 15,
    "timestamp": "2024-01-01T10:00:00Z",
    "success": true
  },
  "message": "Command executed successfully"
}
```

**Examples:**

**Basic Greeting:**
```bash
curl -X POST http://localhost:8080/api/v1/agents/1/execute \
  -H "Content-Type: application/json" \
  -d '{
    "command": "Hello, what can you do?"
  }'
```

**Analysis Request:**
```bash
curl -X POST http://localhost:8080/api/v1/agents/1/execute \
  -H "Content-Type: application/json" \
  -d '{
    "command": "Please analyze this code snippet",
    "context": "function add(a, b) { return a + b; }"
  }'
```

**Help Request:**
```bash
curl -X POST http://localhost:8080/api/v1/agents/1/execute \
  -H "Content-Type: application/json" \
  -d '{
    "command": "I need help with my project"
  }'
```

**Command Intent Examples:**

| Command | Intent | Response Type |
|---------|--------|---------------|
| "Hello" | greeting | Welcome message |
| "Help me" | help | Available commands |
| "What's your status?" | status | System status |
| "Analyze this code" | analyze | Analysis response |
| "Create a function" | create | Creation guidance |
| "Explain recursion" | explain | Explanation |

**Error Responses:**
```json
// Agent not found
HTTP 404 Not Found

// Agent busy
{
  "success": false,
  "error": {
    "code": "COMMAND_EXECUTION_FAILED",
    "message": "Agent is not available. Current status: BUSY"
  }
}

// Invalid command
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Input validation failed",
    "details": {
      "command": "Command content is required"
    }
  }
}

// Execution failure
{
  "success": false,
  "data": {
    "commandId": 2,
    "status": "FAILED",
    "result": null,
    "errorMessage": "Command processing failed: Invalid syntax",
    "executionTimeMs": 5,
    "timestamp": "2024-01-01T10:00:00Z",
    "success": false
  },
  "message": "Command execution failed"
}
```

## 🔍 Query and Filter Endpoints

### 1. Get Agents by Status

Retrieves all agents with a specific status.

**Endpoint:** `GET /agents/status/{status}`

**Path Parameters:**
- `status`: `IDLE|BUSY|OFFLINE|ERROR`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Assistant",
      "type": "GENERAL",
      "status": "IDLE",
      "description": "A helpful assistant",
      "commandCount": 5,
      "createdAt": "2024-01-01T10:00:00Z",
      "updatedAt": "2024-01-01T10:30:00Z"
    }
  ],
  "message": "Agents filtered by status"
}
```

**Examples:**
```bash
# Get all idle agents
curl http://localhost:8080/api/v1/agents/status/IDLE

# Get all busy agents
curl http://localhost:8080/api/v1/agents/status/BUSY

# Get all offline agents
curl http://localhost:8080/api/v1/agents/status/OFFLINE
```

### 2. Get Agents by Type

Retrieves all agents of a specific type.

**Endpoint:** `GET /agents/type/{type}`

**Path Parameters:**
- `type`: `GENERAL|CODE_REVIEWER|DOCUMENTATION|TESTING|ORCHESTRATOR|MONITOR`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Assistant",
      "type": "GENERAL",
      "status": "IDLE",
      "description": "A helpful assistant",
      "commandCount": 5,
      "createdAt": "2024-01-01T10:00:00Z",
      "updatedAt": "2024-01-01T10:30:00Z"
    }
  ],
  "message": "Agents filtered by type"
}
```

**Examples:**
```bash
# Get all general agents
curl http://localhost:8080/api/v1/agents/type/GENERAL

# Get all code reviewer agents
curl http://localhost:8080/api/v1/agents/type/CODE_REVIEWER
```

### 3. Get Available Agents

Retrieves all agents that are currently available (status = IDLE).

**Endpoint:** `GET /agents/available`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Assistant",
      "type": "GENERAL",
      "status": "IDLE",
      "description": "A helpful assistant",
      "commandCount": 5,
      "createdAt": "2024-01-01T10:00:00Z",
      "updatedAt": "2024-01-01T10:30:00Z"
    }
  ],
  "message": "Available agents retrieved"
}
```

**Example:**
```bash
curl http://localhost:8080/api/v1/agents/available
```

## 📊 Analytics and History Endpoints

### 1. Get Agent Command History

Retrieves the command execution history for a specific agent.

**Endpoint:** `GET /agents/{id}/commands`

**Path Parameters:**
- `id`: Agent ID (required)

**Query Parameters:**
- `page` (optional): Page number (0-based, default: 0)
- `size` (optional): Page size (default: 20, max: 100)
- `sort` (optional): Sort criteria (default: `timestamp,desc`)

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "content": "Hello, what can you do?",
        "context": "Testing the agent",
        "result": "Hello! I'm your agent assistant...",
        "status": "COMPLETED",
        "nlpProvider": "mock",
        "executionTimeMs": 15,
        "errorMessage": null,
        "timestamp": "2024-01-01T10:00:00Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 1,
    "totalPages": 1
  },
  "message": "Command history retrieved"
}
```

**Examples:**
```bash
# Get recent commands for agent 1
curl http://localhost:8080/api/v1/agents/1/commands

# Get older commands (page 2)
curl "http://localhost:8080/api/v1/agents/1/commands?page=1&size=10"

# Sort by execution time
curl "http://localhost:8080/api/v1/agents/1/commands?sort=executionTimeMs,desc"
```

### 2. Get Agent Statistics

Retrieves performance statistics for a specific agent.

**Endpoint:** `GET /agents/{id}/statistics`

**Path Parameters:**
- `id`: Agent ID (required)

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "agentId": 1,
    "totalCommands": 10,
    "successfulCommands": 8,
    "failedCommands": 2,
    "successRate": 0.8
  },
  "message": "Agent statistics retrieved"
}
```

**Example:**
```bash
curl http://localhost:8080/api/v1/agents/1/statistics
```

## 🔧 System Health Endpoints

### 1. Agent Controller Health

Simple health check for the agent controller.

**Endpoint:** `GET /agents/health`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": "OK",
  "message": "Agent controller is healthy",
  "timestamp": "2024-01-01T10:00:00Z",
  "version": "1.0.0"
}
```

**Example:**
```bash
curl http://localhost:8080/api/v1/agents/health
```

### 2. System Health (Actuator)

Comprehensive health check including database status.

**Endpoint:** `GET /actuator/health`

**Response:** `200 OK`
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "H2",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 1000000000,
        "free": 500000000,
        "threshold": 10485760,
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

**Example:**
```bash
curl http://localhost:8080/actuator/health
```

## 📈 Metrics Endpoints

### Application Metrics

**Endpoint:** `GET /actuator/metrics`

Lists all available metrics.

**Endpoint:** `GET /actuator/metrics/{metricName}`

Gets specific metric details.

**Examples:**
```bash
# List all metrics
curl http://localhost:8080/actuator/metrics

# JVM memory usage
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP request metrics
curl http://localhost:8080/actuator/metrics/http.server.requests
```

## 🧪 Testing the API

### Using cURL

**Complete Testing Workflow:**
```bash
#!/bin/bash

# 1. Check system health
curl http://localhost:8080/actuator/health

# 2. Create an agent
AGENT=$(curl -s -X POST http://localhost:8080/api/v1/agents \
  -H "Content-Type: application/json" \
  -d '{"name": "TestAgent", "type": "GENERAL", "description": "Test agent"}')

# Extract agent ID (using jq)
AGENT_ID=$(echo $AGENT | jq -r '.data.id')

# 3. Execute commands
curl -X POST http://localhost:8080/api/v1/agents/$AGENT_ID/execute \
  -H "Content-Type: application/json" \
  -d '{"command": "Hello, please help me"}'

curl -X POST http://localhost:8080/api/v1/agents/$AGENT_ID/execute \
  -H "Content-Type: application/json" \
  -d '{"command": "Analyze this code", "context": "function test() { return true; }"}'

# 4. Check command history
curl http://localhost:8080/api/v1/agents/$AGENT_ID/commands

# 5. Get statistics
curl http://localhost:8080/api/v1/agents/$AGENT_ID/statistics

# 6. List all agents
curl http://localhost:8080/api/v1/agents

# 7. Clean up (optional)
curl -X DELETE http://localhost:8080/api/v1/agents/$AGENT_ID
```

### Using Postman

**Collection Structure:**
```
Multi-Agent System API/
├── Agent Management/
│   ├── Create Agent
│   ├── List Agents
│   ├── Get Agent by ID
│   ├── Update Agent
│   └── Delete Agent
├── Command Execution/
│   └── Execute Command
├── Queries/
│   ├── Get Agents by Status
│   ├── Get Agents by Type
│   └── Get Available Agents
├── Analytics/
│   ├── Get Command History
│   └── Get Agent Statistics
└── Health/
    ├── Controller Health
    └── System Health
```

## 🚨 Common Issues and Solutions

### Issue: Agent Not Found
**Problem:** `404 Not Found` when accessing agent
**Solution:** Verify agent ID exists using list endpoint

### Issue: Validation Errors
**Problem:** `400 Bad Request` with validation details
**Solution:** Check request body format and required fields

### Issue: Agent Busy
**Problem:** Cannot execute command on busy agent
**Solution:** Wait for current command completion or check status

### Issue: Database Connection
**Problem:** `500 Internal Server Error` with database errors
**Solution:** Verify H2 console access and database configuration

## 🔐 Security Considerations

### Input Validation
- All inputs are validated at multiple layers
- Maximum field lengths enforced
- SQL injection protection via JPA
- XSS protection through response encoding

### Rate Limiting
*Note: Rate limiting will be implemented in Phase 2*

### Authentication
*Note: JWT authentication will be added in Phase 2*

## 🚀 Performance Tips

### Pagination
- Always use pagination for large result sets
- Default page size is 20, maximum is 100
- Use appropriate sort criteria for your use case

### Query Optimization
- Use specific endpoints (by status, type) instead of filtering client-side
- Cache frequently accessed agent information
- Avoid unnecessary field updates

### Command Execution
- Keep commands concise for better performance
- Use context appropriately - don't send large data
- Monitor execution time via statistics endpoint

---

This API guide provides everything needed to interact with the Phase 1 agent system. For implementation examples and tutorials, see the [Phase 1 Walkthrough](../learning-path/phase1-walkthrough.md).