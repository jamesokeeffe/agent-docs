package com.jamesokeeffe.agentsystem.service.dto;

import com.jamesokeeffe.agentsystem.model.AgentStatus;
import com.jamesokeeffe.agentsystem.model.AgentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Agent operations.
 * 
 * This DTO is used for API requests and responses to transfer agent data
 * between the presentation layer and service layer.
 * 
 * @author James O'Keeffe
 * @version 1.0.0
 * @since 1.0.0
 */
public class AgentDto {

    private Long id;

    @NotBlank(message = "Agent name is required")
    @Size(min = 1, max = 255, message = "Agent name must be between 1 and 255 characters")
    private String name;

    @NotNull(message = "Agent type is required")
    private AgentType type;

    private AgentStatus status;

    private String configuration;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private int commandCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Constructors
    public AgentDto() {}

    public AgentDto(String name, AgentType type) {
        this.name = name;
        this.type = type;
    }

    public AgentDto(String name, AgentType type, String description) {
        this(name, type);
        this.description = description;
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

    public int getCommandCount() {
        return commandCount;
    }

    public void setCommandCount(int commandCount) {
        this.commandCount = commandCount;
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
    public String toString() {
        return "AgentDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", description='" + description + '\'' +
                '}';
    }
}