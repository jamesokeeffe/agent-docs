package com.jamesokeeffe.agentsystem.phase2.plugin;

/**
 * Exception thrown by plugin operations.
 * 
 * @author James O'Keeffe
 * @version 2.0.0
 * @since 2.0.0
 */
public class PluginException extends Exception {

    private final String pluginName;
    private final PluginErrorCode errorCode;

    public PluginException(String message) {
        super(message);
        this.pluginName = null;
        this.errorCode = PluginErrorCode.UNKNOWN;
    }

    public PluginException(String message, Throwable cause) {
        super(message, cause);
        this.pluginName = null;
        this.errorCode = PluginErrorCode.UNKNOWN;
    }

    public PluginException(String pluginName, PluginErrorCode errorCode, String message) {
        super(message);
        this.pluginName = pluginName;
        this.errorCode = errorCode;
    }

    public PluginException(String pluginName, PluginErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.pluginName = pluginName;
        this.errorCode = errorCode;
    }

    public String getPluginName() {
        return pluginName;
    }

    public PluginErrorCode getErrorCode() {
        return errorCode;
    }

    public enum PluginErrorCode {
        INITIALIZATION_FAILED,
        EXECUTION_FAILED,
        CONFIGURATION_INVALID,
        DEPENDENCY_MISSING,
        PERMISSION_DENIED,
        RESOURCE_NOT_FOUND,
        TIMEOUT,
        UNKNOWN
    }
}