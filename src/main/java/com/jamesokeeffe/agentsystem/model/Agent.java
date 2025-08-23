package com.jamesokeeffe.agentsystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Agent entity representing an autonomous agent in the system.
 * 
 * This entity stores the core information about each agent including:
 * - Basic identification (name, type)
 * - Current status and configuration
 * - Audit information (creation/modification timestamps)
 * - Command history
 * 
 * @author James O'Keeffe
 * @version 1.0.0
 * @since 1.0.0
 */
@Entity
@Table(name = "agents", indexes = {
    @Index(name = "idx_agent_name", columnList = "name"),
    @Index(name = "idx_agent_type_status", columnList = "type,status")
})
@EntityListeners(AuditingEntityListener.class)
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Agent name is required")
    @Size(min = 1, max = 255, message = "Agent name must be between 1 and 255 characters")
    @Column(nullable = false, unique = true)
    private String name;

    @NotNull(message = "Agent type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentType type;

    @NotNull(message = "Agent status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentStatus status;

    @Column(columnDefinition = "TEXT")
    private String configuration;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Command> commands = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Agent() {}

    public Agent(String name, AgentType type) {
        this.name = name;
        this.type = type;
        this.status = AgentStatus.IDLE;
    }

    public Agent(String name, AgentType type, String description) {
        this(name, type);
        this.description = description;
    }

    // Business Methods
    
    /**
     * Checks if the agent is currently available to execute commands.
     * 
     * @return true if agent is in IDLE status, false otherwise
     */
    public boolean isAvailable() {
        return status == AgentStatus.IDLE;
    }

    /**
     * Marks the agent as busy (executing a command).
     */
    public void markBusy() {
        this.status = AgentStatus.BUSY;
    }

    /**
     * Marks the agent as idle (available for commands).
     */
    public void markIdle() {
        this.status = AgentStatus.IDLE;
    }

    /**
     * Marks the agent as offline/unavailable.
     */
    public void markOffline() {
        this.status = AgentStatus.OFFLINE;
    }

    /**
     * Adds a command to this agent's command history.
     * 
     * @param command the command to add
     */
    public void addCommand(Command command) {
        commands.add(command);
        command.setAgent(this);
    }

    /**
     * Gets the number of commands executed by this agent.
     * 
     * @return total command count
     */
    public int getCommandCount() {
        return commands.size();
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

    public AgentType getType() {
        return type;
    }

    public void setType(AgentType type) {
        this.type = type;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public void setCommands(List<Command> commands) {
        this.commands = commands;
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

    // Object methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Agent agent = (Agent) o;
        return Objects.equals(id, agent.id) && Objects.equals(name, agent.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Agent{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}