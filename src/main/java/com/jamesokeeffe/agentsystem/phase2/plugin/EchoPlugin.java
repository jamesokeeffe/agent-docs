package com.jamesokeeffe.agentsystem.phase2.plugin;

import java.util.Map;

/**
 * Simple echo plugin that returns the input as output.
 * 
 * @author James O'Keeffe
 * @version 2.0.0
 * @since 2.0.0
 */
public class EchoPlugin implements AgentPlugin {

    private Map<String, Object> configuration;

    @Override
    public void initialize(Map<String, Object> configuration) throws PluginException {
        this.configuration = configuration;
    }

    @Override
    public PluginResult execute(PluginContext context) throws PluginException {
        long startTime = System.currentTimeMillis();
        
        try {
            String message = context.getParameter("message", String.class);
            if (message == null) {
                message = context.getCommand();
            }
            
            return PluginResult.builder()
                .message("Echo: " + message)
                .data("original", message)
                .data("echo", "Echo: " + message)
                .executionTime(System.currentTimeMillis() - startTime)
                .build();
                
        } catch (Exception e) {
            throw new PluginException(getName(), 
                PluginException.PluginErrorCode.EXECUTION_FAILED,
                "Echo plugin execution failed", e);
        }
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }

    @Override
    public String getName() {
        return "echo-plugin";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginType getType() {
        return PluginType.COMMAND_PROCESSOR;
    }

    @Override
    public String getDescription() {
        return "Simple echo plugin that returns the input message";
    }

    @Override
    public boolean supports(String command) {
        return command != null && 
               (command.toLowerCase().contains("echo") || 
                command.toLowerCase().contains("repeat"));
    }

    @Override
    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    @Override
    public boolean validateConfiguration(Map<String, Object> configuration) {
        return true; // No special configuration required
    }
}