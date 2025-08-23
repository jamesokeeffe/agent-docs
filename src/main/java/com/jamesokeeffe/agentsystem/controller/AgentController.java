package com.jamesokeeffe.agentsystem.controller;

import com.jamesokeeffe.agentsystem.model.AgentStatus;
import com.jamesokeeffe.agentsystem.model.AgentType;
import com.jamesokeeffe.agentsystem.model.Command;
import com.jamesokeeffe.agentsystem.service.AgentService;
import com.jamesokeeffe.agentsystem.service.dto.AgentDto;
import com.jamesokeeffe.agentsystem.service.dto.CommandRequest;
import com.jamesokeeffe.agentsystem.service.dto.CommandResult;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * REST Controller for agent management and operations.
 * 
 * This controller provides HTTP endpoints for:
 * - Agent CRUD operations
 * - Command execution
 * - Agent querying and statistics
 * 
 * @author James O'Keeffe
 * @version 1.0.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/agents")
@CrossOrigin(origins = "*") // For development - restrict in production
public class AgentController {

    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    private final AgentService agentService;

    @Autowired
    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    // Agent CRUD Operations

    /**
     * Creates a new agent.
     * 
     * @param agentDto the agent data
     * @return the created agent with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AgentDto>> createAgent(@Valid @RequestBody AgentDto agentDto) {
        logger.info("REST: Creating new agent: {}", agentDto.getName());
        
        try {
            AgentDto createdAgent = agentService.createAgent(agentDto);
            
            return ResponseEntity
                .created(URI.create("/api/v1/agents/" + createdAgent.getId()))
                .body(ApiResponse.success(createdAgent, "Agent created successfully"));
                
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Failed to create agent: {}", e.getMessage());
            return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("AGENT_CREATION_FAILED", e.getMessage()));
        }
    }

    /**
     * Retrieves all agents with pagination.
     * 
     * @param pageable pagination parameters
     * @return page of agents
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AgentDto>>> getAllAgents(
            @PageableDefault(size = 20) Pageable pageable) {
        logger.debug("REST: Retrieving all agents with pagination");
        
        Page<AgentDto> agents = agentService.getAllAgents(pageable);
        return ResponseEntity.ok(ApiResponse.success(agents, "Agents retrieved successfully"));
    }

    /**
     * Retrieves an agent by ID.
     * 
     * @param id the agent ID
     * @return the agent if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AgentDto>> getAgentById(@PathVariable Long id) {
        logger.debug("REST: Retrieving agent by ID: {}", id);
        
        try {
            AgentDto agent = agentService.getAgentById(id);
            return ResponseEntity.ok(ApiResponse.success(agent, "Agent retrieved successfully"));
            
        } catch (RuntimeException e) {
            logger.warn("Agent not found: {}", e.getMessage());
            return ResponseEntity
                .notFound()
                .build();
        }
    }

    /**
     * Updates an existing agent.
     * 
     * @param id the agent ID
     * @param agentDto the updated agent data
     * @return the updated agent
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AgentDto>> updateAgent(
            @PathVariable Long id, 
            @Valid @RequestBody AgentDto agentDto) {
        logger.info("REST: Updating agent with ID: {}", id);
        
        try {
            AgentDto updatedAgent = agentService.updateAgent(id, agentDto);
            return ResponseEntity.ok(ApiResponse.success(updatedAgent, "Agent updated successfully"));
            
        } catch (RuntimeException e) {
            logger.warn("Failed to update agent: {}", e.getMessage());
            
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("AGENT_UPDATE_FAILED", e.getMessage()));
        }
    }

    /**
     * Deletes an agent by ID.
     * 
     * @param id the agent ID
     * @return HTTP 204 if successful
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAgent(@PathVariable Long id) {
        logger.info("REST: Deleting agent with ID: {}", id);
        
        try {
            agentService.deleteAgent(id);
            return ResponseEntity
                .noContent()
                .build();
                
        } catch (RuntimeException e) {
            logger.warn("Failed to delete agent: {}", e.getMessage());
            
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("AGENT_DELETE_FAILED", e.getMessage()));
        }
    }

    // Command Execution

    /**
     * Executes a command on the specified agent.
     * 
     * @param id the agent ID
     * @param commandRequest the command to execute
     * @return the command execution result
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<ApiResponse<CommandResult>> executeCommand(
            @PathVariable Long id,
            @Valid @RequestBody CommandRequest commandRequest) {
        logger.info("REST: Executing command on agent {}: {}", id, commandRequest.getCommand());
        
        try {
            CommandResult result = agentService.executeCommand(id, commandRequest);
            
            HttpStatus status = result.isSuccess() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
            String message = result.isSuccess() ? "Command executed successfully" : "Command execution failed";
            
            return ResponseEntity
                .status(status)
                .body(ApiResponse.success(result, message));
                
        } catch (RuntimeException e) {
            logger.error("Command execution failed: {}", e.getMessage());
            
            if (e.getMessage().contains("not found")) {
                return ResponseEntity
                    .notFound()
                    .build();
            }
            
            return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("COMMAND_EXECUTION_FAILED", e.getMessage()));
        }
    }

    // Query Endpoints

    /**
     * Finds agents by status.
     * 
     * @param status the agent status
     * @return list of agents with the specified status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<AgentDto>>> getAgentsByStatus(@PathVariable AgentStatus status) {
        logger.debug("REST: Finding agents by status: {}", status);
        
        List<AgentDto> agents = agentService.getAgentsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(agents, "Agents filtered by status"));
    }

    /**
     * Finds agents by type.
     * 
     * @param type the agent type
     * @return list of agents of the specified type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<AgentDto>>> getAgentsByType(@PathVariable AgentType type) {
        logger.debug("REST: Finding agents by type: {}", type);
        
        List<AgentDto> agents = agentService.getAgentsByType(type);
        return ResponseEntity.ok(ApiResponse.success(agents, "Agents filtered by type"));
    }

    /**
     * Gets all available agents (status = IDLE).
     * 
     * @return list of available agents
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<AgentDto>>> getAvailableAgents() {
        logger.debug("REST: Finding available agents");
        
        List<AgentDto> agents = agentService.getAvailableAgents();
        return ResponseEntity.ok(ApiResponse.success(agents, "Available agents retrieved"));
    }

    /**
     * Gets command history for an agent.
     * 
     * @param id the agent ID
     * @param pageable pagination parameters
     * @return page of commands for the agent
     */
    @GetMapping("/{id}/commands")
    public ResponseEntity<ApiResponse<Page<Command>>> getAgentCommandHistory(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        logger.debug("REST: Retrieving command history for agent: {}", id);
        
        try {
            Page<Command> commands = agentService.getAgentCommandHistory(id, pageable);
            return ResponseEntity.ok(ApiResponse.success(commands, "Command history retrieved"));
            
        } catch (RuntimeException e) {
            logger.warn("Failed to retrieve command history: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Gets agent statistics.
     * 
     * @param id the agent ID
     * @return agent statistics
     */
    @GetMapping("/{id}/statistics")
    public ResponseEntity<ApiResponse<AgentService.AgentStatistics>> getAgentStatistics(@PathVariable Long id) {
        logger.debug("REST: Retrieving statistics for agent: {}", id);
        
        try {
            AgentService.AgentStatistics stats = agentService.getAgentStatistics(id);
            return ResponseEntity.ok(ApiResponse.success(stats, "Agent statistics retrieved"));
            
        } catch (RuntimeException e) {
            logger.warn("Failed to retrieve agent statistics: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // Helper endpoint for testing
    
    /**
     * Health check endpoint for the controller.
     * 
     * @return simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("OK", "Agent controller is healthy"));
    }
}