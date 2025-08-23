package com.jamesokeeffe.agentsystem.phase2.plugin;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Plugin entity representing a dynamic capability that can be loaded into agents.
 * 
 * Plugins extend agent functionality by providing:
 * - Custom command processors
 * - External API integrations
 * - Specialized AI capabilities
 * - Data transformations
 * 
 * @author James O'Keeffe
 * @version 2.0.0
 * @since 2.0.0
 */
@Entity
@Table(name = "plugins", indexes = {
    @Index(name = "idx_plugin_agent", columnList = "agent_id"),
    @Index(name = "idx_plugin_name", columnList = "name"),
    @Index(name = "idx_plugin_enabled", columnList = "enabled")
})
@EntityListeners(AuditingEntityListener.class)
public class Plugin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Agent ID is required")
    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @NotBlank(message = "Plugin name is required")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Plugin type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PluginType type;

    @Column
    private String version;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String configuration;

    @Column(name = "class_name")
    private String className;

    @Column(name = "jar_path")
    private String jarPath;

    @NotNull(message = "Plugin status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PluginStatus status;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "load_priority")
    private Integer loadPriority = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Plugin() {}

    public Plugin(Long agentId, String name, PluginType type) {
        this.agentId = agentId;
        this.name = name;
        this.type = type;
        this.status = PluginStatus.INSTALLED;
        this.enabled = true;
    }

    // Business Methods
    
    /**
     * Checks if plugin is ready for execution.
     */
    public boolean isActive() {
        return enabled && status == PluginStatus.ACTIVE;
    }

    /**
     * Enables the plugin.
     */
    public void enable() {
        this.enabled = true;
        if (this.status == PluginStatus.INSTALLED) {
            this.status = PluginStatus.ACTIVE;
        }
    }

    /**
     * Disables the plugin.
     */
    public void disable() {
        this.enabled = false;
        if (this.status == PluginStatus.ACTIVE) {
            this.status = PluginStatus.INACTIVE;
        }
    }

    /**
     * Marks plugin as failed with error status.
     */
    public void markFailed() {
        this.status = PluginStatus.FAILED;
        this.enabled = false;
    }

    /**
     * Activates the plugin after successful loading.
     */
    public void activate() {
        this.status = PluginStatus.ACTIVE;
        this.enabled = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PluginType getType() {
        return type;
    }

    public void setType(PluginType type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getJarPath() {
        return jarPath;
    }

    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }

    public PluginStatus getStatus() {
        return status;
    }

    public void setStatus(PluginStatus status) {
        this.status = status;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getLoadPriority() {
        return loadPriority;
    }

    public void setLoadPriority(Integer loadPriority) {
        this.loadPriority = loadPriority;
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
        Plugin plugin = (Plugin) o;
        return Objects.equals(id, plugin.id) && Objects.equals(name, plugin.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Plugin{" +
                "id=" + id +
                ", agentId=" + agentId +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", enabled=" + enabled +
                '}';
    }
}