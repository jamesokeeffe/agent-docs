package com.jamesokeeffe.agentsystem.phase4.mcp;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * MCP Tool entity representing a tool available via Model Context Protocol.
 * 
 * Tools are external functions or services that agents can invoke:
 * - Function definitions and schemas
 * - Input/output specifications
 * - Access control and permissions
 * - Usage tracking and metrics
 * 
 * @author James O'Keeffe
 * @version 4.0.0
 * @since 4.0.0
 */
@Entity
@Table(name = "mcp_tools", indexes = {
    @Index(name = "idx_tool_name", columnList = "name"),
    @Index(name = "idx_tool_agent", columnList = "agent_id"),
    @Index(name = "idx_tool_enabled", columnList = "enabled")
})
@EntityListeners(AuditingEntityListener.class)
public class MCPTool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tool name is required")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Tool schema is required")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String schema; // JSON schema for tool parameters

    @Column(name = "agent_id")
    private Long agentId;

    @NotNull(message = "Tool type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MCPToolType type;

    @Column(name = "endpoint_url")
    private String endpointUrl;

    @Column(columnDefinition = "TEXT")
    private String configuration; // Tool-specific configuration

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "usage_count")
    private Integer usageCount = 0;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "average_execution_time_ms")
    private Long averageExecutionTimeMs;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public MCPTool() {}

    public MCPTool(String name, String schema, MCPToolType type) {
        this.name = name;
        this.schema = schema;
        this.type = type;
    }

    public MCPTool(String name, String description, String schema, MCPToolType type) {
        this(name, schema, type);
        this.description = description;
    }

    // Business Methods
    
    /**
     * Checks if tool is available for use.
     */
    public boolean isAvailable() {
        return enabled;
    }

    /**
     * Records tool usage.
     */
    public void recordUsage(long executionTimeMs) {
        this.usageCount++;
        this.lastUsedAt = LocalDateTime.now();
        
        // Update average execution time
        if (this.averageExecutionTimeMs == null) {
            this.averageExecutionTimeMs = executionTimeMs;
        } else {
            this.averageExecutionTimeMs = (this.averageExecutionTimeMs + executionTimeMs) / 2;
        }
    }

    /**
     * Enables the tool.
     */
    public void enable() {
        this.enabled = true;
    }

    /**
     * Disables the tool.
     */
    public void disable() {
        this.enabled = false;
    }

    /**
     * Gets tool usage rate (uses per day).
     */
    public double getUsageRate() {
        if (usageCount == 0 || createdAt == null) return 0.0;
        
        long daysSinceCreation = java.time.Duration.between(createdAt, LocalDateTime.now()).toDays();
        if (daysSinceCreation == 0) daysSinceCreation = 1;
        
        return (double) usageCount / daysSinceCreation;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public MCPToolType getType() {
        return type;
    }

    public void setType(MCPToolType type) {
        this.type = type;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public Long getAverageExecutionTimeMs() {
        return averageExecutionTimeMs;
    }

    public void setAverageExecutionTimeMs(Long averageExecutionTimeMs) {
        this.averageExecutionTimeMs = averageExecutionTimeMs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MCPTool mcpTool = (MCPTool) o;
        return Objects.equals(id, mcpTool.id) && Objects.equals(name, mcpTool.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "MCPTool{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", enabled=" + enabled +
                ", usageCount=" + usageCount +
                ", usageRate=" + String.format("%.2f/day", getUsageRate()) +
                '}';
    }
}