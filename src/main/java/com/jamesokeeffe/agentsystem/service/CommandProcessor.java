package com.jamesokeeffe.agentsystem.service;

import com.jamesokeeffe.agentsystem.service.dto.CommandResult;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Service for processing natural language commands.
 * 
 * This service handles the parsing and execution of natural language commands
 * sent to agents. In Phase 1, it provides basic command recognition and processing.
 * Future phases will integrate with AI APIs for more sophisticated NLP.
 * 
 * @author James O'Keeffe
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
public class CommandProcessor {

    // Simple pattern matching for Phase 1 - will be replaced with AI in Phase 2
    private static final Map<Pattern, String> COMMAND_PATTERNS = new ConcurrentHashMap<>();

    static {
        // Basic greeting patterns
        COMMAND_PATTERNS.put(Pattern.compile("(?i).*(hello|hi|hey).*"), "greeting");
        COMMAND_PATTERNS.put(Pattern.compile("(?i).*(help|assist|support).*"), "help");
        
        // Status and information patterns
        COMMAND_PATTERNS.put(Pattern.compile("(?i).*(status|health|state).*"), "status");
        COMMAND_PATTERNS.put(Pattern.compile("(?i).*(info|information|details).*"), "info");
        
        // Task execution patterns
        COMMAND_PATTERNS.put(Pattern.compile("(?i).*(analyze|review|check).*"), "analyze");
        COMMAND_PATTERNS.put(Pattern.compile("(?i).*(create|generate|build).*"), "create");
        COMMAND_PATTERNS.put(Pattern.compile("(?i).*(explain|describe|tell).*"), "explain");
        
        // System operations
        COMMAND_PATTERNS.put(Pattern.compile("(?i).*(shutdown|stop|exit).*"), "shutdown");
        COMMAND_PATTERNS.put(Pattern.compile("(?i).*(restart|reset|reload).*"), "restart");
    }

    /**
     * Processes a natural language command and returns a structured result.
     * 
     * @param command the natural language command
     * @param context optional context information
     * @param nlpProvider the NLP provider to use (mock for Phase 1)
     * @return the command execution result
     */
    public CommandResult processCommand(String command, String context, String nlpProvider) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Parse the command to determine intent
            String intent = parseCommandIntent(command);
            
            // Execute based on intent
            String result = executeCommand(intent, command, context);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            CommandResult commandResult = CommandResult.success(result);
            commandResult.setExecutionTimeMs(executionTime);
            
            return commandResult;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            CommandResult result = CommandResult.failure("Command processing failed: " + e.getMessage());
            result.setExecutionTimeMs(executionTime);
            
            return result;
        }
    }

    /**
     * Parses the command to determine the intent using pattern matching.
     * 
     * @param command the command text
     * @return the detected intent
     */
    private String parseCommandIntent(String command) {
        if (command == null || command.trim().isEmpty()) {
            return "unknown";
        }

        for (Map.Entry<Pattern, String> entry : COMMAND_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(command).matches()) {
                return entry.getValue();
            }
        }

        return "unknown";
    }

    /**
     * Executes the command based on the detected intent.
     * 
     * @param intent the command intent
     * @param command the original command
     * @param context the execution context
     * @return the execution result
     */
    private String executeCommand(String intent, String command, String context) {
        return switch (intent) {
            case "greeting" -> handleGreeting(command, context);
            case "help" -> handleHelp(command, context);
            case "status" -> handleStatus(command, context);
            case "info" -> handleInfo(command, context);
            case "analyze" -> handleAnalyze(command, context);
            case "create" -> handleCreate(command, context);
            case "explain" -> handleExplain(command, context);
            case "shutdown" -> handleShutdown(command, context);
            case "restart" -> handleRestart(command, context);
            default -> handleUnknown(command, context);
        };
    }

    // Command handlers for different intents

    private String handleGreeting(String command, String context) {
        return "Hello! I'm your agent assistant. I'm ready to help you with various tasks. " +
               "You can ask me to analyze, create, explain, or provide information about different topics.";
    }

    private String handleHelp(String command, String context) {
        return """
               I can help you with the following types of commands:
               
               • Greetings: Say hello or hi
               • Analysis: Ask me to analyze, review, or check something
               • Creation: Ask me to create, generate, or build something
               • Information: Ask for info, details, or explanations
               • Status: Check my status or health
               • System: Basic system operations
               
               Just type your request in natural language and I'll do my best to assist you!
               """;
    }

    private String handleStatus(String command, String context) {
        return "Status: OPERATIONAL - I'm running normally and ready to process commands. " +
               "All systems are functioning correctly.";
    }

    private String handleInfo(String command, String context) {
        return "I'm a Phase 1 Agent in the Multi-Agent System. " +
               "I can process natural language commands and provide basic assistance. " +
               "In future phases, I'll gain more advanced capabilities like AI integration, " +
               "plugin support, and multi-agent coordination.";
    }

    private String handleAnalyze(String command, String context) {
        String target = extractTarget(command, "analyze");
        return String.format("Analysis of '%s': This appears to be a request for analysis. " +
                            "In Phase 1, I provide basic analysis capabilities. " +
                            "More sophisticated analysis will be available in future phases with AI integration.",
                            target);
    }

    private String handleCreate(String command, String context) {
        String target = extractTarget(command, "create");
        return String.format("Creation request for '%s': I acknowledge your request to create something. " +
                            "In Phase 1, I can provide guidance and structure. " +
                            "Enhanced creation capabilities will be available in future phases.",
                            target);
    }

    private String handleExplain(String command, String context) {
        String target = extractTarget(command, "explain");
        return String.format("Explanation of '%s': This is a request for explanation. " +
                            "I can provide basic information and context. " +
                            "More detailed explanations will be available with AI integration in Phase 2.",
                            target);
    }

    private String handleShutdown(String command, String context) {
        return "Shutdown command received. Note: I cannot actually shut down the system - " +
               "this is a simulated response for demonstration purposes.";
    }

    private String handleRestart(String command, String context) {
        return "Restart command received. Note: I cannot actually restart the system - " +
               "this is a simulated response for demonstration purposes.";
    }

    private String handleUnknown(String command, String context) {
        return String.format("I received your command: '%s' but I'm not sure how to handle it. " +
                           "Please try rephrasing your request or ask for help to see available commands. " +
                           "Context provided: %s",
                           command, context != null ? context : "none");
    }

    /**
     * Extracts the target object from a command after a specific verb.
     * 
     * @param command the full command
     * @param verb the verb to look for
     * @return the extracted target
     */
    private String extractTarget(String command, String verb) {
        String lowerCommand = command.toLowerCase();
        int verbIndex = lowerCommand.indexOf(verb);
        
        if (verbIndex >= 0 && verbIndex + verb.length() < command.length()) {
            String afterVerb = command.substring(verbIndex + verb.length()).trim();
            return afterVerb.isEmpty() ? "unspecified target" : afterVerb;
        }
        
        return "unspecified target";
    }

    /**
     * Validates if a command is valid for processing.
     * 
     * @param command the command to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return false;
        }
        
        // Additional validation rules can be added here
        return command.length() <= 5000; // Match entity constraint
    }
}