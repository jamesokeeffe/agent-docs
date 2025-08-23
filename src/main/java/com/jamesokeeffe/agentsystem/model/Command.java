package com.jamesokeeffe.agentsystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Command entity representing a command executed by an agent.
 * 
 * Commands store the history of all interactions with agents including:
 * - The natural language command text
 * - Execution context and results
 * - Timing information
 * - Association with the executing agent
 * 
 * @author James O'Keeffe
 * @version 1.0.0
 * @since 1.0.0
 */
@Entity
@Table(name = "commands", indexes = {
    @Index(name = "idx_command_agent_id", columnList = "agent_id"),
    @Index(name = "idx_command_timestamp", columnList = "timestamp"),
    @Index(name = "idx_command_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class Command {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Agent is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @NotBlank(message = "Command content is required")
    @Size(min = 1, max = 5000, message = "Command content must be between 1 and 5000 characters")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String context;

    @Column(columnDefinition = "TEXT")
    private String result;

    @NotNull(message = "Command status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommandStatus status;

    @Column(name = "nlp_provider")
    private String nlpProvider;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // Constructors
    public Command() {}

    public Command(Agent agent, String content) {
        this.agent = agent;
        this.content = content;
        this.status = CommandStatus.PENDING;
    }

    public Command(Agent agent, String content, String context) {
        this(agent, content);
        this.context = context;
    }

    // Business Methods

    /**
     * Marks the command as being executed.
     */
    public void markExecuting() {
        this.status = CommandStatus.EXECUTING;
    }

    /**
     * Marks the command as successfully completed.
     * 
     * @param result the command execution result
     * @param executionTimeMs the execution time in milliseconds
     */
    public void markCompleted(String result, Long executionTimeMs) {
        this.status = CommandStatus.COMPLETED;
        this.result = result;
        this.executionTimeMs = executionTimeMs;
        this.errorMessage = null;
    }

    /**
     * Marks the command as failed.
     * 
     * @param errorMessage the error message describing the failure
     * @param executionTimeMs the execution time in milliseconds before failure
     */
    public void markFailed(String errorMessage, Long executionTimeMs) {
        this.status = CommandStatus.FAILED;
        this.errorMessage = errorMessage;
        this.executionTimeMs = executionTimeMs;
    }

    /**
     * Checks if the command execution was successful.
     * 
     * @return true if the command completed successfully
     */
    public boolean isSuccessful() {
        return status == CommandStatus.COMPLETED;
    }

    /**
     * Checks if the command is still being processed.
     * 
     * @return true if the command is pending or executing
     */
    public boolean isInProgress() {
        return status == CommandStatus.PENDING || status == CommandStatus.EXECUTING;
    }

    /**
     * Gets a summary of the command for logging or display.
     * 
     * @return a brief summary string
     */
    public String getSummary() {
        String truncatedContent = content.length() > 50 
            ? content.substring(0, 50) + "..." 
            : content;
        return String.format("Command[%d]: %s (%s)", id, truncatedContent, status);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public CommandStatus getStatus() {
        return status;
    }

    public void setStatus(CommandStatus status) {
        this.status = status;
    }

    public String getNlpProvider() {
        return nlpProvider;
    }

    public void setNlpProvider(String nlpProvider) {
        this.nlpProvider = nlpProvider;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    // Object methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Command command = (Command) o;
        return Objects.equals(id, command.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Command{" +
                "id=" + id +
                ", content='" + (content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content) + '\'' +
                ", status=" + status +
                ", timestamp=" + timestamp +
                '}';
    }
}