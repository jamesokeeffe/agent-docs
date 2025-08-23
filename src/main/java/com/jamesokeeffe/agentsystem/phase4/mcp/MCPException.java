package com.jamesokeeffe.agentsystem.phase4.mcp;

/**
 * Exception thrown by MCP operations.
 * 
 * @author James O'Keeffe
 * @version 4.0.0
 * @since 4.0.0
 */
public class MCPException extends Exception {

    private final MCPErrorCode errorCode;

    public MCPException(String message) {
        super(message);
        this.errorCode = MCPErrorCode.UNKNOWN;
    }

    public MCPException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = MCPErrorCode.UNKNOWN;
    }

    public MCPException(MCPErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public MCPException(MCPErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public MCPErrorCode getErrorCode() {
        return errorCode;
    }

    public enum MCPErrorCode {
        TOOL_NOT_FOUND,
        RESOURCE_NOT_FOUND,
        ACCESS_DENIED,
        INVALID_SCHEMA,
        EXECUTION_FAILED,
        PROTOCOL_ERROR,
        TIMEOUT,
        UNKNOWN
    }
}