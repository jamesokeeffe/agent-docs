package com.jamesokeeffe.agentsystem.phase2.plugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Time plugin that provides current time information.
 * 
 * @author James O'Keeffe
 * @version 2.0.0
 * @since 2.0.0
 */
public class TimePlugin implements AgentPlugin {

    private Map<String, Object> configuration;

    @Override
    public void initialize(Map<String, Object> configuration) throws PluginException {
        this.configuration = configuration;
    }

    @Override
    public PluginResult execute(PluginContext context) throws PluginException {
        long startTime = System.currentTimeMillis();
        
        try {
            LocalDateTime now = LocalDateTime.now();
            String format = context.getParameter("format", String.class);
            
            String timeString;
            if (format != null) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                    timeString = now.format(formatter);
                } catch (Exception e) {
                    timeString = now.toString();
                }
            } else {
                timeString = now.toString();
            }
            
            return PluginResult.builder()
                .message("Current time: " + timeString)
                .data("timestamp", now.toString())
                .data("formatted", timeString)
                .data("epoch", System.currentTimeMillis())
                .executionTime(System.currentTimeMillis() - startTime)
                .build();
                
        } catch (Exception e) {
            throw new PluginException(getName(), 
                PluginException.PluginErrorCode.EXECUTION_FAILED,
                "Time plugin execution failed", e);
        }
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }

    @Override
    public String getName() {
        return "time-plugin";
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
        return "Plugin that provides current time and date information";
    }

    @Override
    public boolean supports(String command) {
        return command != null && 
               (command.toLowerCase().contains("time") || 
                command.toLowerCase().contains("date") ||
                command.toLowerCase().contains("clock"));
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