package com.jamesokeeffe.agentsystem.phase4.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MCP service functionality.
 * 
 * @author James O'Keeffe
 * @version 4.0.0
 * @since 4.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
class MCPServiceTest {

    @Test
    void testMCPToolCreation() {
        // Test tool creation
        MCPTool tool = new MCPTool("test-tool", "Test tool description", "{}", MCPToolType.FUNCTION);
        
        assertNotNull(tool);
        assertEquals("test-tool", tool.getName());
        assertEquals(MCPToolType.FUNCTION, tool.getType());
        assertTrue(tool.isAvailable());
        assertEquals(0, tool.getUsageCount());
    }

    @Test
    void testMCPResourceCreation() {
        // Test resource creation
        MCPResource resource = new MCPResource("test://resource", "Test Resource", MCPAccessLevel.READ);
        
        assertNotNull(resource);
        assertEquals("test://resource", resource.getUri());
        assertEquals("Test Resource", resource.getName());
        assertEquals(MCPAccessLevel.READ, resource.getAccessLevel());
        assertTrue(resource.isReadable());
        assertFalse(resource.isWritable());
    }

    @Test
    void testMCPExecutionResult() {
        // Test successful execution result
        Map<String, Object> result = Map.of("output", "success", "value", 42);
        MCPExecutionResult successResult = MCPExecutionResult.success(result, 100);
        
        assertTrue(successResult.isSuccess());
        assertEquals(result, successResult.getResult());
        assertEquals(100, successResult.getExecutionTimeMs());
        
        // Test failure result
        MCPExecutionResult failureResult = MCPExecutionResult.failure("Test error");
        
        assertFalse(failureResult.isSuccess());
        assertEquals("Test error", failureResult.getError());
        assertNull(failureResult.getResult());
    }
}