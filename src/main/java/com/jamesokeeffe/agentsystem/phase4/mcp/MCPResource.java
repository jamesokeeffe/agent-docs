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
 * MCP Resource entity representing a resource accessible via Model Context Protocol.
 * 
 * Resources are data sources that agents can access:
 * - Files, documents, and media
 * - Database records and collections
 * - Web resources and APIs
 * - Streams and real-time data
 * 
 * @author James O'Keeffe
 * @version 4.0.0
 * @since 4.0.0
 */
@Entity
@Table(name = "mcp_resources", indexes = {
    @Index(name = "idx_resource_uri", columnList = "uri"),
    @Index(name = "idx_resource_agent", columnList = "agent_id"),
    @Index(name = "idx_resource_type", columnList = "mime_type")
})
@EntityListeners(AuditingEntityListener.class)
public class MCPResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Resource URI is required")
    @Column(nullable = false, length = 500)
    private String uri;

    @Column
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "agent_id")
    private Long agentId;

    @NotNull(message = "Access level is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false)
    private MCPAccessLevel accessLevel;

    @Column(name = "content_size")
    private Long contentSize;

    @Column(name = "last_modified")
    private LocalDateTime lastModified;

    @Column(name = "access_count")
    private Integer accessCount = 0;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(columnDefinition = "TEXT")
    private String metadata; // Additional resource metadata

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public MCPResource() {}

    public MCPResource(String uri, String name, MCPAccessLevel accessLevel) {
        this.uri = uri;
        this.name = name;
        this.accessLevel = accessLevel;
    }

    public MCPResource(String uri, String name, String mimeType, MCPAccessLevel accessLevel) {
        this(uri, name, accessLevel);
        this.mimeType = mimeType;
    }

    // Business Methods
    
    /**
     * Checks if resource is readable.
     */
    public boolean isReadable() {
        return accessLevel == MCPAccessLevel.READ || 
               accessLevel == MCPAccessLevel.READ_WRITE;
    }

    /**
     * Checks if resource is writable.
     */
    public boolean isWritable() {
        return accessLevel == MCPAccessLevel.WRITE || 
               accessLevel == MCPAccessLevel.READ_WRITE;
    }

    /**
     * Records resource access.
     */
    public void recordAccess() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * Updates resource metadata.
     */
    public void updateMetadata(Long size, LocalDateTime modified) {
        this.contentSize = size;
        this.lastModified = modified;
    }

    /**
     * Gets resource access rate (accesses per day).
     */
    public double getAccessRate() {
        if (accessCount == 0 || createdAt == null) return 0.0;
        
        long daysSinceCreation = java.time.Duration.between(createdAt, LocalDateTime.now()).toDays();
        if (daysSinceCreation == 0) daysSinceCreation = 1;
        
        return (double) accessCount / daysSinceCreation;
    }

    /**
     * Gets human-readable size.
     */
    public String getFormattedSize() {
        if (contentSize == null) return "Unknown";
        
        long size = contentSize;
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
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

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public MCPAccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(MCPAccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    public Long getContentSize() {
        return contentSize;
    }

    public void setContentSize(Long contentSize) {
        this.contentSize = contentSize;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public Integer getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(Integer accessCount) {
        this.accessCount = accessCount;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
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
        MCPResource that = (MCPResource) o;
        return Objects.equals(id, that.id) && Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, uri);
    }

    @Override
    public String toString() {
        return "MCPResource{" +
                "id=" + id +
                ", uri='" + uri + '\'' +
                ", name='" + name + '\'' +
                ", accessLevel=" + accessLevel +
                ", size='" + getFormattedSize() + '\'' +
                ", accessCount=" + accessCount +
                '}';
    }
}