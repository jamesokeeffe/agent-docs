package com.jamesokeeffe.agentsystem.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for command execution requests.
 * 
 * This DTO is used when a client wants to execute a command on an agent.
 * It contains the command content and optional context information.
 * 
 * @author James O'Keeffe
 * @version 1.0.0
 * @since 1.0.0
 */
public class CommandRequest {

    @NotBlank(message = "Command content is required")
    @Size(min = 1, max = 5000, message = "Command content must be between 1 and 5000 characters")
    private String command;

    @Size(max = 2000, message = "Context must not exceed 2000 characters")
    private String context;

    private String nlpProvider;

    // Constructors
    public CommandRequest() {}

    public CommandRequest(String command) {
        this.command = command;
    }

    public CommandRequest(String command, String context) {
        this.command = command;
        this.context = context;
    }

    // Getters and Setters
    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getNlpProvider() {
        return nlpProvider;
    }

    public void setNlpProvider(String nlpProvider) {
        this.nlpProvider = nlpProvider;
    }

    @Override
    public String toString() {
        return "CommandRequest{" +
                "command='" + (command != null && command.length() > 50 ? command.substring(0, 50) + "..." : command) + '\'' +
                ", context='" + context + '\'' +
                ", nlpProvider='" + nlpProvider + '\'' +
                '}';
    }
}