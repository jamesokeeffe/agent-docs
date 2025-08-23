package com.jamesokeeffe.agentsystem.phase4.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing MCP (Model Context Protocol) tools and resources.
 * 
 * Responsibilities:
 * - Tool and resource registration and discovery
 * - Access control and permissions
 * - Usage tracking and metrics
 * - Protocol communication and serialization
 * 
 * @author James O'Keeffe
 * @version 4.0.0
 * @since 4.0.0
 */
@Service
public class MCPService {

    private static final Logger logger = LoggerFactory.getLogger(MCPService.class);

    private final MCPToolRepository toolRepository;
    private final MCPResourceRepository resourceRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public MCPService(MCPToolRepository toolRepository,
                     MCPResourceRepository resourceRepository,
                     ObjectMapper objectMapper) {
        this.toolRepository = toolRepository;
        this.resourceRepository = resourceRepository;
        this.objectMapper = objectMapper;
        
        // Initialize with built-in tools and resources
        initializeBuiltInTools();
        initializeBuiltInResources();
    }

    /**
     * Initialize built-in MCP tools.
     */
    private void initializeBuiltInTools() {
        // Create sample tools if none exist
        if (toolRepository.count() == 0) {
            try {
                // File system tool
                MCPTool fileTool = new MCPTool(
                    "file_read",
                    "Read file contents",
                    createFileReadSchema(),
                    MCPToolType.FILE_SYSTEM
                );
                toolRepository.save(fileTool);

                // API call tool
                MCPTool apiTool = new MCPTool(
                    "http_request",
                    "Make HTTP requests",
                    createHttpRequestSchema(),
                    MCPToolType.API
                );
                toolRepository.save(apiTool);

                // Database query tool
                MCPTool dbTool = new MCPTool(
                    "database_query",
                    "Execute database queries",
                    createDatabaseQuerySchema(),
                    MCPToolType.DATABASE
                );
                toolRepository.save(dbTool);

                logger.info("Initialized {} built-in MCP tools", 3);
            } catch (Exception e) {
                logger.error("Failed to initialize built-in tools: {}", e.getMessage());
            }
        }
    }

    /**
     * Initialize built-in MCP resources.
     */
    private void initializeBuiltInResources() {
        // Create sample resources if none exist
        if (resourceRepository.count() == 0) {
            try {
                // Configuration resource
                MCPResource configResource = new MCPResource(
                    "config://application.properties",
                    "Application Configuration",
                    "text/plain",
                    MCPAccessLevel.READ
                );
                resourceRepository.save(configResource);

                // Log resource
                MCPResource logResource = new MCPResource(
                    "logs://application.log",
                    "Application Logs", 
                    "text/plain",
                    MCPAccessLevel.READ
                );
                resourceRepository.save(logResource);

                // Data resource
                MCPResource dataResource = new MCPResource(
                    "data://agents",
                    "Agent Data",
                    "application/json",
                    MCPAccessLevel.READ_WRITE
                );
                resourceRepository.save(dataResource);

                logger.info("Initialized {} built-in MCP resources", 3);
            } catch (Exception e) {
                logger.error("Failed to initialize built-in resources: {}", e.getMessage());
            }
        }
    }

    /**
     * Executes an MCP tool with given parameters.
     */
    public MCPExecutionResult executeTool(String toolName, Map<String, Object> parameters) {
        try {
            MCPTool tool = toolRepository.findByNameAndEnabledTrue(toolName)
                .orElseThrow(() -> new MCPException("Tool not found or disabled: " + toolName));

            long startTime = System.currentTimeMillis();
            
            // Execute tool based on type
            Map<String, Object> result = switch (tool.getType()) {
                case FILE_SYSTEM -> executeFileSystemTool(tool, parameters);
                case API -> executeApiTool(tool, parameters);
                case DATABASE -> executeDatabaseTool(tool, parameters);
                case COMPUTATION -> executeComputationTool(tool, parameters);
                default -> executeGenericTool(tool, parameters);
            };

            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record usage
            tool.recordUsage(executionTime);
            toolRepository.save(tool);

            logger.debug("Executed tool {} in {}ms", toolName, executionTime);
            return MCPExecutionResult.success(result, executionTime);

        } catch (Exception e) {
            logger.error("Tool execution failed for {}: {}", toolName, e.getMessage());
            return MCPExecutionResult.failure(e.getMessage());
        }
    }

    /**
     * Accesses an MCP resource.
     */
    public MCPResourceContent accessResource(String uri, MCPAccessLevel requiredLevel) throws MCPException {
        try {
            MCPResource resource = resourceRepository.findByUri(uri)
                .orElseThrow(() -> new MCPException("Resource not found: " + uri));

            // Check access permissions
            if (!hasAccess(resource, requiredLevel)) {
                throw new MCPException("Insufficient permissions for resource: " + uri);
            }

            // Record access
            resource.recordAccess();
            resourceRepository.save(resource);

            // Retrieve content based on URI scheme
            String content = retrieveResourceContent(resource);
            
            logger.debug("Accessed resource: {}", uri);
            return new MCPResourceContent(resource, content);

        } catch (Exception e) {
            logger.error("Resource access failed for {}: {}", uri, e.getMessage());
            throw new MCPException("Failed to access resource: " + uri, e);
        }
    }

    /**
     * Checks if access is allowed for a resource.
     */
    private boolean hasAccess(MCPResource resource, MCPAccessLevel requiredLevel) {
        return switch (requiredLevel) {
            case READ -> resource.isReadable();
            case WRITE -> resource.isWritable();
            case READ_WRITE -> resource.isReadable() && resource.isWritable();
            case EXECUTE, ADMIN -> resource.getAccessLevel() == requiredLevel;
        };
    }

    /**
     * Retrieves resource content based on URI.
     */
    private String retrieveResourceContent(MCPResource resource) {
        String uri = resource.getUri();
        
        // Simplified content retrieval - in reality this would handle different URI schemes
        if (uri.startsWith("config://")) {
            return "# Configuration content\napp.name=agent-system\napp.version=4.0.0";
        } else if (uri.startsWith("logs://")) {
            return "INFO - Application started\nDEBUG - Processing request\nINFO - Request completed";
        } else if (uri.startsWith("data://")) {
            return "{\"agents\": [{\"id\": 1, \"name\": \"Assistant\", \"status\": \"active\"}]}";
        } else {
            return "Resource content for: " + uri;
        }
    }

    /**
     * Execute file system tool.
     */
    private Map<String, Object> executeFileSystemTool(MCPTool tool, Map<String, Object> parameters) {
        Map<String, Object> result = new HashMap<>();
        String filePath = (String) parameters.get("path");
        
        result.put("tool", tool.getName());
        result.put("path", filePath);
        result.put("content", "File content for: " + filePath);
        result.put("size", 1024);
        result.put("lastModified", LocalDateTime.now().toString());
        
        return result;
    }

    /**
     * Execute API tool.
     */
    private Map<String, Object> executeApiTool(MCPTool tool, Map<String, Object> parameters) {
        Map<String, Object> result = new HashMap<>();
        String url = (String) parameters.get("url");
        String method = (String) parameters.getOrDefault("method", "GET");
        
        result.put("tool", tool.getName());
        result.put("url", url);
        result.put("method", method);
        result.put("status", 200);
        result.put("response", "API response for: " + url);
        
        return result;
    }

    /**
     * Execute database tool.
     */
    private Map<String, Object> executeDatabaseTool(MCPTool tool, Map<String, Object> parameters) {
        Map<String, Object> result = new HashMap<>();
        String query = (String) parameters.get("query");
        
        result.put("tool", tool.getName());
        result.put("query", query);
        result.put("rows", List.of(
            Map.of("id", 1, "name", "Agent 1"),
            Map.of("id", 2, "name", "Agent 2")
        ));
        result.put("rowCount", 2);
        
        return result;
    }

    /**
     * Execute computation tool.
     */
    private Map<String, Object> executeComputationTool(MCPTool tool, Map<String, Object> parameters) {
        Map<String, Object> result = new HashMap<>();
        
        result.put("tool", tool.getName());
        result.put("computation", "Sample computation result");
        result.put("result", 42);
        
        return result;
    }

    /**
     * Execute generic tool.
     */
    private Map<String, Object> executeGenericTool(MCPTool tool, Map<String, Object> parameters) {
        Map<String, Object> result = new HashMap<>();
        
        result.put("tool", tool.getName());
        result.put("parameters", parameters);
        result.put("message", "Generic tool execution completed");
        
        return result;
    }

    /**
     * Creates schema for file read tool.
     */
    private String createFileReadSchema() {
        try {
            Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                    "path", Map.of(
                        "type", "string",
                        "description", "File path to read"
                    )
                ),
                "required", List.of("path")
            );
            return objectMapper.writeValueAsString(schema);
        } catch (Exception e) {
            return "{\"type\": \"object\"}";
        }
    }

    /**
     * Creates schema for HTTP request tool.
     */
    private String createHttpRequestSchema() {
        try {
            Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                    "url", Map.of(
                        "type", "string",
                        "description", "URL to request"
                    ),
                    "method", Map.of(
                        "type", "string",
                        "enum", List.of("GET", "POST", "PUT", "DELETE"),
                        "default", "GET"
                    )
                ),
                "required", List.of("url")
            );
            return objectMapper.writeValueAsString(schema);
        } catch (Exception e) {
            return "{\"type\": \"object\"}";
        }
    }

    /**
     * Creates schema for database query tool.
     */
    private String createDatabaseQuerySchema() {
        try {
            Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                    "query", Map.of(
                        "type", "string",
                        "description", "SQL query to execute"
                    )
                ),
                "required", List.of("query")
            );
            return objectMapper.writeValueAsString(schema);
        } catch (Exception e) {
            return "{\"type\": \"object\"}";
        }
    }

    // Service methods
    public List<MCPTool> getAllTools() {
        return toolRepository.findAll();
    }

    public List<MCPTool> getEnabledTools() {
        return toolRepository.findByEnabledTrue();
    }

    public List<MCPResource> getAllResources() {
        return resourceRepository.findAll();
    }

    public MCPTool createTool(String name, String description, String schema, MCPToolType type) {
        MCPTool tool = new MCPTool(name, description, schema, type);
        return toolRepository.save(tool);
    }

    public MCPResource createResource(String uri, String name, String mimeType, MCPAccessLevel accessLevel) {
        MCPResource resource = new MCPResource(uri, name, mimeType, accessLevel);
        return resourceRepository.save(resource);
    }

    public long getToolCount() {
        return toolRepository.count();
    }

    public long getResourceCount() {
        return resourceRepository.count();
    }
}