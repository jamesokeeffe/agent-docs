# Phase 3 MCP Protocol Guide

## Overview

This guide covers implementing the Model Context Protocol (MCP) for standardized agent communication and tool integration.

## MCP Protocol Fundamentals

### 1. What is MCP?

The Model Context Protocol (MCP) is an open standard for connecting AI models and agents with external tools and data sources. It provides:

- **Standardized Communication**: Consistent interface across different AI tools
- **Tool Integration**: Seamless connection to external services and data
- **Resource Management**: Structured access to files, databases, and APIs
- **Schema Validation**: Type-safe interactions with tools and resources

### 2. MCP Message Format

MCP uses JSON-RPC 2.0 for message formatting:

```json
{
  "jsonrpc": "2.0",
  "id": "request-id",
  "method": "tools/call",
  "params": {
    "name": "file_read",
    "arguments": {
      "path": "/path/to/file.txt"
    }
  }
}
```

Response format:
```json
{
  "jsonrpc": "2.0",
  "id": "request-id",
  "result": {
    "content": [
      {
        "type": "text",
        "text": "File content here..."
      }
    ]
  }
}
```

## MCP Server Implementation

### 1. Core MCP Server

```java
@Component
@Slf4j
public class MCPServer implements MCPProtocolHandler {
    
    private final MCPToolRegistry toolRegistry;
    private final MCPResourceManager resourceManager;
    private final MCPSchemaValidator schemaValidator;
    private final MCPConnectionManager connectionManager;
    
    @Override
    public MCPResponse handleRequest(MCPRequest request) {
        try {
            // Validate request format
            schemaValidator.validate(request);
            
            // Log request for monitoring
            logRequest(request);
            
            // Route to appropriate handler
            return routeRequest(request);
            
        } catch (MCPValidationException e) {
            return MCPResponse.error(MCPErrorCode.INVALID_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Error processing MCP request", e);
            return MCPResponse.error(MCPErrorCode.INTERNAL_ERROR, "Internal server error");
        }
    }
    
    private MCPResponse routeRequest(MCPRequest request) {
        String method = request.getMethod();
        
        switch (method) {
            case "initialize":
                return handleInitialize(request);
            case "tools/list":
                return handleToolsList(request);
            case "tools/call":
                return handleToolCall(request);
            case "resources/list":
                return handleResourcesList(request);
            case "resources/read":
                return handleResourceRead(request);
            case "prompts/list":
                return handlePromptsList(request);
            case "prompts/get":
                return handlePromptsGet(request);
            default:
                return MCPResponse.error(MCPErrorCode.METHOD_NOT_FOUND, 
                    "Unknown method: " + method);
        }
    }
    
    private MCPResponse handleInitialize(MCPRequest request) {
        MCPInitializeParams params = request.getParams(MCPInitializeParams.class);
        
        // Validate protocol version compatibility
        if (!isCompatibleVersion(params.getProtocolVersion())) {
            return MCPResponse.error(MCPErrorCode.INVALID_PARAMS, 
                "Unsupported protocol version: " + params.getProtocolVersion());
        }
        
        // Create connection context
        String connectionId = connectionManager.createConnection(params);
        
        MCPInitializeResult result = MCPInitializeResult.builder()
            .protocolVersion("2024-11-05")
            .serverInfo(MCPServerInfo.builder()
                .name("Multi-Agent System MCP Server")
                .version("1.0.0")
                .build())
            .capabilities(MCPCapabilities.builder()
                .tools(true)
                .resources(true)
                .prompts(true)
                .logging(true)
                .build())
            .build();
        
        return MCPResponse.success(result);
    }
    
    private MCPResponse handleToolCall(MCPRequest request) {
        MCPToolCallParams params = request.getParams(MCPToolCallParams.class);
        
        String toolName = params.getName();
        Map<String, Object> arguments = params.getArguments();
        
        try {
            MCPToolResult result = toolRegistry.executeTool(toolName, arguments);
            return MCPResponse.success(result);
        } catch (MCPToolNotFoundException e) {
            return MCPResponse.error(MCPErrorCode.TOOL_NOT_FOUND, 
                "Tool not found: " + toolName);
        } catch (MCPToolExecutionException e) {
            return MCPResponse.error(MCPErrorCode.TOOL_EXECUTION_ERROR, 
                e.getMessage());
        }
    }
}
```

### 2. Tool Registry Implementation

```java
@Component
public class MCPToolRegistry {
    
    private final Map<String, MCPTool> tools = new ConcurrentHashMap<>();
    private final MCPToolValidator toolValidator;
    private final MCPToolExecutor toolExecutor;
    private final MCPToolMetrics toolMetrics;
    
    @PostConstruct
    public void initialize() {
        registerBuiltInTools();
        log.info("MCP Tool Registry initialized with {} tools", tools.size());
    }
    
    public void registerTool(MCPTool tool) {
        // Validate tool definition
        toolValidator.validate(tool);
        
        // Register tool
        tools.put(tool.getName(), tool);
        
        // Initialize tool metrics
        toolMetrics.initializeMetrics(tool.getName());
        
        log.info("Registered MCP tool: {} ({})", tool.getName(), tool.getDescription());
    }
    
    public MCPToolResult executeTool(String toolName, Map<String, Object> arguments) {
        MCPTool tool = tools.get(toolName);
        if (tool == null) {
            throw new MCPToolNotFoundException("Tool not found: " + toolName);
        }
        
        // Record execution metrics
        long startTime = System.currentTimeMillis();
        toolMetrics.incrementExecutionCount(toolName);
        
        try {
            // Validate arguments against tool schema
            toolValidator.validateArguments(tool, arguments);
            
            // Execute tool
            MCPToolResult result = toolExecutor.execute(tool, arguments);
            
            // Record success metrics
            long executionTime = System.currentTimeMillis() - startTime;
            toolMetrics.recordExecutionTime(toolName, executionTime);
            toolMetrics.incrementSuccessCount(toolName);
            
            return result;
            
        } catch (Exception e) {
            // Record error metrics
            toolMetrics.incrementErrorCount(toolName);
            throw new MCPToolExecutionException("Tool execution failed: " + toolName, e);
        }
    }
    
    public List<MCPToolDefinition> getAllTools() {
        return tools.values().stream()
            .map(MCPTool::getDefinition)
            .collect(Collectors.toList());
    }
    
    private void registerBuiltInTools() {
        // File system operations
        registerTool(new FileSystemTool());
        
        // Database operations
        registerTool(new DatabaseTool());
        
        // API client
        registerTool(new APIClientTool());
        
        // Computation tools
        registerTool(new ComputationTool());
        
        // Git operations
        registerTool(new GitTool());
        
        // Code analysis
        registerTool(new CodeAnalysisTool());
        
        // Documentation generation
        registerTool(new DocumentationTool());
        
        // Test generation
        registerTool(new TestGeneratorTool());
        
        // Metrics and monitoring
        registerTool(new MetricsTool());
        
        // Log analysis
        registerTool(new LogAnalysisTool());
    }
}
```

## MCP Tool Implementations

### 1. File System Tool

```java
@Component
public class FileSystemTool implements MCPTool {
    
    private static final String TOOL_NAME = "filesystem";
    private static final String TOOL_DESCRIPTION = "File system operations (read, write, list, search)";
    
    @Override
    public String getName() {
        return TOOL_NAME;
    }
    
    @Override
    public String getDescription() {
        return TOOL_DESCRIPTION;
    }
    
    @Override
    public MCPToolDefinition getDefinition() {
        return MCPToolDefinition.builder()
            .name(TOOL_NAME)
            .description(TOOL_DESCRIPTION)
            .inputSchema(createInputSchema())
            .build();
    }
    
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
            case "search":
                return searchFiles(arguments);
            case "mkdir":
                return createDirectory(arguments);
            case "delete":
                return deleteFile(arguments);
            default:
                throw new MCPToolExecutionException("Unknown operation: " + operation);
        }
    }
    
    private MCPToolResult readFile(Map<String, Object> arguments) {
        String path = (String) arguments.get("path");
        String encoding = (String) arguments.getOrDefault("encoding", "UTF-8");
        
        try {
            Path filePath = Paths.get(path);
            
            // Security check - prevent path traversal
            validateFilePath(filePath);
            
            String content = Files.readString(filePath, Charset.forName(encoding));
            
            return MCPToolResult.success(Map.of(
                "content", content,
                "size", Files.size(filePath),
                "lastModified", Files.getLastModifiedTime(filePath).toString()
            ));
            
        } catch (IOException e) {
            throw new MCPToolExecutionException("Failed to read file: " + path, e);
        }
    }
    
    private MCPToolResult writeFile(Map<String, Object> arguments) {
        String path = (String) arguments.get("path");
        String content = (String) arguments.get("content");
        String encoding = (String) arguments.getOrDefault("encoding", "UTF-8");
        boolean append = (Boolean) arguments.getOrDefault("append", false);
        
        try {
            Path filePath = Paths.get(path);
            validateFilePath(filePath);
            
            // Ensure parent directories exist
            Files.createDirectories(filePath.getParent());
            
            if (append) {
                Files.writeString(filePath, content, Charset.forName(encoding), 
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.writeString(filePath, content, Charset.forName(encoding));
            }
            
            return MCPToolResult.success(Map.of(
                "path", path,
                "size", Files.size(filePath),
                "operation", append ? "append" : "write"
            ));
            
        } catch (IOException e) {
            throw new MCPToolExecutionException("Failed to write file: " + path, e);
        }
    }
    
    private MCPToolResult listDirectory(Map<String, Object> arguments) {
        String path = (String) arguments.get("path");
        boolean recursive = (Boolean) arguments.getOrDefault("recursive", false);
        String pattern = (String) arguments.get("pattern");
        
        try {
            Path dirPath = Paths.get(path);
            validateFilePath(dirPath);
            
            List<Map<String, Object>> files = new ArrayList<>();
            
            if (recursive) {
                Files.walk(dirPath)
                    .filter(p -> pattern == null || p.getFileName().toString().matches(pattern))
                    .forEach(p -> files.add(createFileInfo(p)));
            } else {
                Files.list(dirPath)
                    .filter(p -> pattern == null || p.getFileName().toString().matches(pattern))
                    .forEach(p -> files.add(createFileInfo(p)));
            }
            
            return MCPToolResult.success(Map.of(
                "path", path,
                "files", files,
                "count", files.size()
            ));
            
        } catch (IOException e) {
            throw new MCPToolExecutionException("Failed to list directory: " + path, e);
        }
    }
    
    private Map<String, Object> createFileInfo(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            
            return Map.of(
                "name", path.getFileName().toString(),
                "path", path.toString(),
                "size", attrs.size(),
                "isDirectory", attrs.isDirectory(),
                "lastModified", attrs.lastModifiedTime().toString(),
                "created", attrs.creationTime().toString()
            );
        } catch (IOException e) {
            return Map.of(
                "name", path.getFileName().toString(),
                "path", path.toString(),
                "error", "Unable to read file attributes"
            );
        }
    }
    
    private void validateFilePath(Path path) {
        // Implement security checks to prevent path traversal attacks
        String normalizedPath = path.normalize().toString();
        if (normalizedPath.contains("..") || normalizedPath.startsWith("/etc") || 
            normalizedPath.startsWith("/sys") || normalizedPath.startsWith("/proc")) {
            throw new SecurityException("Access denied to path: " + path);
        }
    }
    
    private MCPSchema createInputSchema() {
        return MCPSchema.object()
            .property("operation", MCPSchema.string()
                .enumValues("read", "write", "list", "search", "mkdir", "delete")
                .description("File system operation to perform"))
            .property("path", MCPSchema.string()
                .description("File or directory path"))
            .property("content", MCPSchema.string()
                .description("Content to write (for write operations)"))
            .property("encoding", MCPSchema.string()
                .defaultValue("UTF-8")
                .description("File encoding"))
            .property("recursive", MCPSchema.bool()
                .defaultValue(false)
                .description("Recursive operation (for list/search)"))
            .property("pattern", MCPSchema.string()
                .description("File name pattern (regex)"))
            .required("operation", "path")
            .build();
    }
}
```

### 2. Database Tool

```java
@Component
public class DatabaseTool implements MCPTool {
    
    private static final String TOOL_NAME = "database";
    private static final String TOOL_DESCRIPTION = "Database operations (query, update, schema)";
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public String getName() {
        return TOOL_NAME;
    }
    
    @Override
    public String getDescription() {
        return TOOL_DESCRIPTION;
    }
    
    @Override
    public MCPToolDefinition getDefinition() {
        return MCPToolDefinition.builder()
            .name(TOOL_NAME)
            .description(TOOL_DESCRIPTION)
            .inputSchema(createInputSchema())
            .build();
    }
    
    @Override
    public MCPToolResult execute(Map<String, Object> arguments) {
        String operation = (String) arguments.get("operation");
        
        switch (operation) {
            case "query":
                return executeQuery(arguments);
            case "update":
                return executeUpdate(arguments);
            case "schema":
                return getSchema(arguments);
            case "tables":
                return listTables(arguments);
            case "indexes":
                return listIndexes(arguments);
            default:
                throw new MCPToolExecutionException("Unknown operation: " + operation);
        }
    }
    
    private MCPToolResult executeQuery(Map<String, Object> arguments) {
        String sql = (String) arguments.get("sql");
        List<Object> parameters = (List<Object>) arguments.getOrDefault("parameters", List.of());
        Integer limit = (Integer) arguments.get("limit");
        
        try {
            // Validate SQL to prevent dangerous operations
            validateQuerySql(sql);
            
            // Apply limit if specified
            if (limit != null && limit > 0) {
                sql = applySqlLimit(sql, limit);
            }
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, parameters.toArray());
            
            return MCPToolResult.success(Map.of(
                "results", results,
                "rowCount", results.size(),
                "sql", sql
            ));
            
        } catch (Exception e) {
            throw new MCPToolExecutionException("Query execution failed", e);
        }
    }
    
    private MCPToolResult executeUpdate(Map<String, Object> arguments) {
        String sql = (String) arguments.get("sql");
        List<Object> parameters = (List<Object>) arguments.getOrDefault("parameters", List.of());
        
        try {
            // Validate SQL to ensure it's an update/insert/delete
            validateUpdateSql(sql);
            
            int rowsAffected = jdbcTemplate.update(sql, parameters.toArray());
            
            return MCPToolResult.success(Map.of(
                "rowsAffected", rowsAffected,
                "sql", sql
            ));
            
        } catch (Exception e) {
            throw new MCPToolExecutionException("Update execution failed", e);
        }
    }
    
    private MCPToolResult getSchema(Map<String, Object> arguments) {
        String tableName = (String) arguments.get("tableName");
        
        try {
            String sql = """
                SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, CHARACTER_MAXIMUM_LENGTH
                FROM INFORMATION_SCHEMA.COLUMNS 
                WHERE TABLE_NAME = ? 
                ORDER BY ORDINAL_POSITION
                """;
            
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(sql, tableName);
            
            return MCPToolResult.success(Map.of(
                "tableName", tableName,
                "columns", columns,
                "columnCount", columns.size()
            ));
            
        } catch (Exception e) {
            throw new MCPToolExecutionException("Failed to get schema for table: " + tableName, e);
        }
    }
    
    private void validateQuerySql(String sql) {
        String upperSql = sql.trim().toUpperCase();
        if (!upperSql.startsWith("SELECT") && !upperSql.startsWith("WITH")) {
            throw new SecurityException("Only SELECT and WITH queries are allowed");
        }
        
        // Check for dangerous keywords
        String[] dangerousKeywords = {"DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "CREATE", "TRUNCATE"};
        for (String keyword : dangerousKeywords) {
            if (upperSql.contains(keyword)) {
                throw new SecurityException("Dangerous keyword detected: " + keyword);
            }
        }
    }
    
    private void validateUpdateSql(String sql) {
        String upperSql = sql.trim().toUpperCase();
        if (!upperSql.startsWith("UPDATE") && !upperSql.startsWith("INSERT") && !upperSql.startsWith("DELETE")) {
            throw new SecurityException("Only UPDATE, INSERT, and DELETE statements are allowed");
        }
        
        // Prevent dangerous operations
        if (upperSql.contains("DROP") || upperSql.contains("ALTER") || upperSql.contains("CREATE")) {
            throw new SecurityException("DDL statements are not allowed");
        }
    }
    
    private String applySqlLimit(String sql, int limit) {
        // Simple limit application - in production, use a proper SQL parser
        if (!sql.toUpperCase().contains("LIMIT")) {
            return sql + " LIMIT " + limit;
        }
        return sql;
    }
    
    private MCPSchema createInputSchema() {
        return MCPSchema.object()
            .property("operation", MCPSchema.string()
                .enumValues("query", "update", "schema", "tables", "indexes")
                .description("Database operation to perform"))
            .property("sql", MCPSchema.string()
                .description("SQL statement to execute"))
            .property("parameters", MCPSchema.array(MCPSchema.any())
                .description("SQL parameters"))
            .property("limit", MCPSchema.integer()
                .description("Maximum number of rows to return"))
            .property("tableName", MCPSchema.string()
                .description("Table name for schema operations"))
            .required("operation")
            .build();
    }
}
```

### 3. API Client Tool

```java
@Component
public class APIClientTool implements MCPTool {
    
    private static final String TOOL_NAME = "api_client";
    private static final String TOOL_DESCRIPTION = "HTTP API client for external service integration";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public APIClientTool() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        
        // Configure timeout and other settings
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        restTemplate.setRequestFactory(factory);
    }
    
    @Override
    public String getName() {
        return TOOL_NAME;
    }
    
    @Override
    public String getDescription() {
        return TOOL_DESCRIPTION;
    }
    
    @Override
    public MCPToolDefinition getDefinition() {
        return MCPToolDefinition.builder()
            .name(TOOL_NAME)
            .description(TOOL_DESCRIPTION)
            .inputSchema(createInputSchema())
            .build();
    }
    
    @Override
    public MCPToolResult execute(Map<String, Object> arguments) {
        String method = (String) arguments.get("method");
        String url = (String) arguments.get("url");
        Map<String, String> headers = (Map<String, String>) arguments.getOrDefault("headers", Map.of());
        Object body = arguments.get("body");
        Integer timeout = (Integer) arguments.get("timeout");
        
        try {
            // Validate URL
            validateUrl(url);
            
            // Create HTTP headers
            HttpHeaders httpHeaders = new HttpHeaders();
            headers.forEach(httpHeaders::set);
            
            // Set content type if not specified
            if (!httpHeaders.containsKey(HttpHeaders.CONTENT_TYPE) && body != null) {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            }
            
            // Create request entity
            HttpEntity<?> requestEntity = new HttpEntity<>(body, httpHeaders);
            
            // Configure timeout if specified
            if (timeout != null) {
                configureTimeout(timeout);
            }
            
            // Execute request
            ResponseEntity<String> response = executeRequest(method, url, requestEntity);
            
            // Parse response
            Map<String, Object> responseData = parseResponse(response);
            
            return MCPToolResult.success(responseData);
            
        } catch (Exception e) {
            throw new MCPToolExecutionException("API request failed: " + url, e);
        }
    }
    
    private ResponseEntity<String> executeRequest(String method, String url, HttpEntity<?> requestEntity) {
        switch (method.toUpperCase()) {
            case "GET":
                return restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
            case "POST":
                return restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            case "PUT":
                return restTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);
            case "DELETE":
                return restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
            case "PATCH":
                return restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, String.class);
            case "HEAD":
                return restTemplate.exchange(url, HttpMethod.HEAD, requestEntity, String.class);
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
    }
    
    private Map<String, Object> parseResponse(ResponseEntity<String> response) {
        Map<String, Object> result = new HashMap<>();
        
        result.put("statusCode", response.getStatusCode().value());
        result.put("statusText", response.getStatusCode().getReasonPhrase());
        
        // Parse headers
        Map<String, String> responseHeaders = new HashMap<>();
        response.getHeaders().forEach((key, values) -> 
            responseHeaders.put(key, String.join(", ", values)));
        result.put("headers", responseHeaders);
        
        // Parse body
        String body = response.getBody();
        if (body != null && !body.isEmpty()) {
            try {
                // Try to parse as JSON
                Object parsedBody = objectMapper.readValue(body, Object.class);
                result.put("body", parsedBody);
            } catch (Exception e) {
                // Return as plain text if not valid JSON
                result.put("body", body);
            }
        }
        
        return result;
    }
    
    private void validateUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new SecurityException("Only HTTP and HTTPS URLs are allowed");
            }
            
            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                throw new IllegalArgumentException("Invalid URL: missing host");
            }
            
            // Prevent access to internal networks
            if (isInternalAddress(host)) {
                throw new SecurityException("Access to internal addresses is not allowed");
            }
            
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URL format: " + url);
        }
    }
    
    private boolean isInternalAddress(String host) {
        // Check for localhost and internal IP ranges
        return host.equals("localhost") || 
               host.equals("127.0.0.1") ||
               host.startsWith("192.168.") ||
               host.startsWith("10.") ||
               host.startsWith("172.16.") ||
               host.startsWith("172.17.") ||
               host.startsWith("172.18.") ||
               host.startsWith("172.19.") ||
               host.startsWith("172.20.") ||
               host.startsWith("172.21.") ||
               host.startsWith("172.22.") ||
               host.startsWith("172.23.") ||
               host.startsWith("172.24.") ||
               host.startsWith("172.25.") ||
               host.startsWith("172.26.") ||
               host.startsWith("172.27.") ||
               host.startsWith("172.28.") ||
               host.startsWith("172.29.") ||
               host.startsWith("172.30.") ||
               host.startsWith("172.31.");
    }
    
    private void configureTimeout(int timeout) {
        HttpComponentsClientHttpRequestFactory factory = 
            (HttpComponentsClientHttpRequestFactory) restTemplate.getRequestFactory();
        factory.setReadTimeout(timeout * 1000);
    }
    
    private MCPSchema createInputSchema() {
        return MCPSchema.object()
            .property("method", MCPSchema.string()
                .enumValues("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD")
                .description("HTTP method"))
            .property("url", MCPSchema.string()
                .description("Request URL"))
            .property("headers", MCPSchema.object()
                .description("HTTP headers"))
            .property("body", MCPSchema.any()
                .description("Request body"))
            .property("timeout", MCPSchema.integer()
                .description("Request timeout in seconds"))
            .required("method", "url")
            .build();
    }
}
```

## MCP Resource Management

### 1. Resource Manager

```java
@Component
public class MCPResourceManager {
    
    private final Map<String, MCPResource> resources = new ConcurrentHashMap<>();
    private final MCPResourceValidator resourceValidator;
    private final MCPPermissionManager permissionManager;
    
    public void registerResource(MCPResource resource) {
        resourceValidator.validate(resource);
        resources.put(resource.getUri(), resource);
        log.info("Registered MCP resource: {}", resource.getUri());
    }
    
    public MCPResource getResource(String uri, String mimeType) {
        MCPResource resource = resources.get(uri);
        if (resource == null) {
            throw new MCPResourceNotFoundException("Resource not found: " + uri);
        }
        
        // Check permissions
        if (!permissionManager.hasReadAccess(uri)) {
            throw new MCPPermissionException("Access denied to resource: " + uri);
        }
        
        // Apply MIME type filter if specified
        if (mimeType != null && !resource.getMimeType().equals(mimeType)) {
            throw new MCPResourceException("Resource MIME type mismatch");
        }
        
        return resource;
    }
    
    public List<MCPResourceInfo> listResources(String uriPattern) {
        return resources.values().stream()
            .filter(resource -> permissionManager.hasReadAccess(resource.getUri()))
            .filter(resource -> uriPattern == null || resource.getUri().matches(uriPattern))
            .map(this::createResourceInfo)
            .collect(Collectors.toList());
    }
    
    private MCPResourceInfo createResourceInfo(MCPResource resource) {
        return MCPResourceInfo.builder()
            .uri(resource.getUri())
            .name(resource.getName())
            .description(resource.getDescription())
            .mimeType(resource.getMimeType())
            .lastModified(resource.getLastModified())
            .size(resource.getSize())
            .build();
    }
}
```

This MCP implementation provides a robust foundation for standardized agent communication and tool integration, enabling sophisticated multi-agent systems with protocol compliance and extensibility.