package com.jamesokeeffe.agentsystem.phase4.mcp;

/**
 * Content of an MCP resource.
 * 
 * @author James O'Keeffe
 * @version 4.0.0
 * @since 4.0.0
 */
public class MCPResourceContent {

    private final MCPResource resource;
    private final String content;

    public MCPResourceContent(MCPResource resource, String content) {
        this.resource = resource;
        this.content = content;
    }

    public MCPResource getResource() {
        return resource;
    }

    public String getContent() {
        return content;
    }

    public String getUri() {
        return resource.getUri();
    }

    public String getName() {
        return resource.getName();
    }

    public String getMimeType() {
        return resource.getMimeType();
    }

    public int getContentLength() {
        return content != null ? content.length() : 0;
    }
}