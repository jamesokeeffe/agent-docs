package com.jamesokeeffe.agentsystem.phase4.mcp;

import com.jamesokeeffe.agentsystem.controller.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for MCP (Model Context Protocol) operations.
 * 
 * Provides endpoints for:
 * - Tool and resource discovery
 * - Tool execution and resource access
 * - MCP protocol management
 * - Usage metrics and monitoring
 * 
 * @author James O'Keeffe
 * @version 4.0.0
 * @since 4.0.0
 */
@RestController
@RequestMapping("/api/v1/mcp")
@CrossOrigin(origins = "*")
public class MCPController {

    private static final Logger logger = LoggerFactory.getLogger(MCPController.class);

    private final MCPService mcpService;

    @Autowired
    public MCPController(MCPService mcpService) {
        this.mcpService = mcpService;
    }

    /**
     * Get all available tools.
     */
    @GetMapping("/tools")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<MCPTool>>> getTools() {
        try {
            List<MCPTool> tools = mcpService.getEnabledTools();
            logger.debug("Retrieved {} MCP tools", tools.size());
            return ResponseEntity.ok(ApiResponse.success(tools));
        } catch (Exception e) {
            logger.error("Error retrieving MCP tools: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("TOOL_RETRIEVAL_ERROR", "Failed to retrieve tools"));
        }
    }

    /**
     * Execute a tool with given parameters.
     */
    @PostMapping("/tools/{toolName}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<MCPExecutionResult>> executeTool(
            @PathVariable String toolName,
            @RequestBody(required = false) Map<String, Object> parameters) {
        try {
            if (parameters == null) {
                parameters = Map.of();
            }
            
            MCPExecutionResult result = mcpService.executeTool(toolName, parameters);
            
            if (result.isSuccess()) {
                logger.debug("Executed MCP tool {} successfully", toolName);
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                logger.warn("MCP tool {} execution failed: {}", toolName, result.getError());
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TOOL_EXECUTION_FAILED", result.getError()));
            }
        } catch (Exception e) {
            logger.error("Error executing MCP tool {}: {}", toolName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("TOOL_EXECUTION_ERROR", "Failed to execute tool"));
        }
    }

    /**
     * Get all available resources.
     */
    @GetMapping("/resources")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<MCPResource>>> getResources() {
        try {
            List<MCPResource> resources = mcpService.getAllResources();
            logger.debug("Retrieved {} MCP resources", resources.size());
            return ResponseEntity.ok(ApiResponse.success(resources));
        } catch (Exception e) {
            logger.error("Error retrieving MCP resources: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("RESOURCE_RETRIEVAL_ERROR", "Failed to retrieve resources"));
        }
    }

    /**
     * Access a resource by URI.
     */
    @GetMapping("/resources/{uri}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<MCPResourceContent>> getResource(
            @PathVariable String uri,
            @RequestParam(defaultValue = "READ") String accessLevel) {
        try {
            MCPAccessLevel level = MCPAccessLevel.valueOf(accessLevel.toUpperCase());
            MCPResourceContent content = mcpService.accessResource(uri, level);
            
            logger.debug("Accessed MCP resource: {}", uri);
            return ResponseEntity.ok(ApiResponse.success(content));
        } catch (MCPException e) {
            logger.warn("MCP resource access failed for {}: {}", uri, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("RESOURCE_ACCESS_DENIED", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid access level: {}", accessLevel);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_ACCESS_LEVEL", "Invalid access level: " + accessLevel));
        } catch (Exception e) {
            logger.error("Error accessing MCP resource {}: {}", uri, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("RESOURCE_ACCESS_ERROR", "Failed to access resource"));
        }
    }

    /**
     * Create a new tool.
     */
    @PostMapping("/tools")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MCPTool>> createTool(@Valid @RequestBody CreateToolRequest request) {
        try {
            MCPTool tool = mcpService.createTool(
                request.getName(),
                request.getDescription(),
                request.getSchema(),
                request.getType()
            );
            
            logger.info("Created MCP tool: {}", tool.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tool));
        } catch (Exception e) {
            logger.error("Error creating MCP tool: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("TOOL_CREATION_ERROR", "Failed to create tool"));
        }
    }

    /**
     * Create a new resource.
     */
    @PostMapping("/resources")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MCPResource>> createResource(@Valid @RequestBody CreateResourceRequest request) {
        try {
            MCPResource resource = mcpService.createResource(
                request.getUri(),
                request.getName(),
                request.getMimeType(),
                request.getAccessLevel()
            );
            
            logger.info("Created MCP resource: {}", resource.getUri());
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(resource));
        } catch (Exception e) {
            logger.error("Error creating MCP resource: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("RESOURCE_CREATION_ERROR", "Failed to create resource"));
        }
    }

    /**
     * Get MCP statistics.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MCPStats>> getStats() {
        try {
            MCPStats stats = new MCPStats(
                mcpService.getToolCount(),
                mcpService.getResourceCount()
            );
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            logger.error("Error retrieving MCP stats: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("STATS_ERROR", "Failed to retrieve MCP statistics"));
        }
    }

    /**
     * Get tool types.
     */
    @GetMapping("/tool-types")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<MCPToolType[]>> getToolTypes() {
        try {
            MCPToolType[] types = MCPToolType.values();
            return ResponseEntity.ok(ApiResponse.success(types));
        } catch (Exception e) {
            logger.error("Error retrieving tool types: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("TOOL_TYPES_ERROR", "Failed to retrieve tool types"));
        }
    }

    /**
     * Get access levels.
     */
    @GetMapping("/access-levels")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<MCPAccessLevel[]>> getAccessLevels() {
        try {
            MCPAccessLevel[] levels = MCPAccessLevel.values();
            return ResponseEntity.ok(ApiResponse.success(levels));
        } catch (Exception e) {
            logger.error("Error retrieving access levels: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("ACCESS_LEVELS_ERROR", "Failed to retrieve access levels"));
        }
    }

    // DTOs
    public static class CreateToolRequest {
        private String name;
        private String description;
        private String schema;
        private MCPToolType type;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSchema() { return schema; }
        public void setSchema(String schema) { this.schema = schema; }
        public MCPToolType getType() { return type; }
        public void setType(MCPToolType type) { this.type = type; }
    }

    public static class CreateResourceRequest {
        private String uri;
        private String name;
        private String mimeType;
        private MCPAccessLevel accessLevel;

        // Getters and setters
        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        public MCPAccessLevel getAccessLevel() { return accessLevel; }
        public void setAccessLevel(MCPAccessLevel accessLevel) { this.accessLevel = accessLevel; }
    }

    public static class MCPStats {
        private final long toolCount;
        private final long resourceCount;

        public MCPStats(long toolCount, long resourceCount) {
            this.toolCount = toolCount;
            this.resourceCount = resourceCount;
        }

        public long getToolCount() { return toolCount; }
        public long getResourceCount() { return resourceCount; }
    }
}