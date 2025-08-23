package com.jamesokeeffe.agentsystem.service;

import com.jamesokeeffe.agentsystem.model.*;
import com.jamesokeeffe.agentsystem.repository.AgentRepository;
import com.jamesokeeffe.agentsystem.repository.CommandRepository;
import com.jamesokeeffe.agentsystem.service.dto.AgentDto;
import com.jamesokeeffe.agentsystem.service.dto.CommandRequest;
import com.jamesokeeffe.agentsystem.service.dto.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class for agent management and operations.
 * 
 * This service provides business logic for:
 * - Agent CRUD operations
 * - Command execution and processing
 * - Agent state management
 * - Command history and statistics
 * 
 * @author James O'Keeffe
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
@Transactional
public class AgentService {

    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);

    private final AgentRepository agentRepository;
    private final CommandRepository commandRepository;
    private final CommandProcessor commandProcessor;
    
    @Value("${agent.system.max-agents:100}")
    private int maxAgents;
    
    @Value("${agent.nlp-provider:mock}")
    private String defaultNlpProvider;

    @Autowired
    public AgentService(AgentRepository agentRepository, 
                       CommandRepository commandRepository,
                       CommandProcessor commandProcessor) {
        this.agentRepository = agentRepository;
        this.commandRepository = commandRepository;
        this.commandProcessor = commandProcessor;
    }

    // Agent CRUD Operations

    /**
     * Creates a new agent.
     * 
     * @param agentDto the agent data
     * @return the created agent
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException if maximum agents exceeded
     */
    public AgentDto createAgent(AgentDto agentDto) {
        logger.info("Creating new agent: {}", agentDto.getName());

        // Validate agent limit
        long currentCount = agentRepository.count();
        if (currentCount >= maxAgents) {
            throw new IllegalStateException("Maximum number of agents (" + maxAgents + ") exceeded");
        }

        // Check for duplicate names
        if (agentRepository.existsByName(agentDto.getName())) {
            throw new IllegalArgumentException("Agent with name '" + agentDto.getName() + "' already exists");
        }

        // Create and save agent
        Agent agent = mapToEntity(agentDto);
        agent.setStatus(AgentStatus.IDLE); // New agents start as idle
        
        Agent savedAgent = agentRepository.save(agent);
        
        logger.info("Successfully created agent with ID: {}", savedAgent.getId());
        return mapToDto(savedAgent);
    }

    /**
     * Retrieves all agents with pagination.
     * 
     * @param pageable pagination information
     * @return page of agents
     */
    @Transactional(readOnly = true)
    public Page<AgentDto> getAllAgents(Pageable pageable) {
        logger.debug("Retrieving all agents with pagination: {}", pageable);
        
        Page<Agent> agents = agentRepository.findAll(pageable);
        return agents.map(this::mapToDto);
    }

    /**
     * Retrieves an agent by ID.
     * 
     * @param id the agent ID
     * @return the agent if found
     * @throws AgentNotFoundException if agent not found
     */
    @Transactional(readOnly = true)
    public AgentDto getAgentById(Long id) {
        logger.debug("Retrieving agent by ID: {}", id);
        
        Agent agent = agentRepository.findById(id)
            .orElseThrow(() -> new AgentNotFoundException("Agent with ID " + id + " not found"));
        
        return mapToDto(agent);
    }

    /**
     * Retrieves an agent by name.
     * 
     * @param name the agent name
     * @return the agent if found
     * @throws AgentNotFoundException if agent not found
     */
    @Transactional(readOnly = true)
    public AgentDto getAgentByName(String name) {
        logger.debug("Retrieving agent by name: {}", name);
        
        Agent agent = agentRepository.findByName(name)
            .orElseThrow(() -> new AgentNotFoundException("Agent with name '" + name + "' not found"));
        
        return mapToDto(agent);
    }

    /**
     * Updates an existing agent.
     * 
     * @param id the agent ID
     * @param agentDto the updated agent data
     * @return the updated agent
     * @throws AgentNotFoundException if agent not found
     */
    public AgentDto updateAgent(Long id, AgentDto agentDto) {
        logger.info("Updating agent with ID: {}", id);
        
        Agent existingAgent = agentRepository.findById(id)
            .orElseThrow(() -> new AgentNotFoundException("Agent with ID " + id + " not found"));

        // Check for name conflicts (if name is being changed)
        if (!existingAgent.getName().equals(agentDto.getName()) && 
            agentRepository.existsByName(agentDto.getName())) {
            throw new IllegalArgumentException("Agent with name '" + agentDto.getName() + "' already exists");
        }

        // Update fields
        existingAgent.setName(agentDto.getName());
        existingAgent.setType(agentDto.getType());
        existingAgent.setDescription(agentDto.getDescription());
        existingAgent.setConfiguration(agentDto.getConfiguration());

        Agent savedAgent = agentRepository.save(existingAgent);
        
        logger.info("Successfully updated agent with ID: {}", savedAgent.getId());
        return mapToDto(savedAgent);
    }

    /**
     * Deletes an agent by ID.
     * 
     * @param id the agent ID
     * @throws AgentNotFoundException if agent not found
     * @throws IllegalStateException if agent is currently busy
     */
    public void deleteAgent(Long id) {
        logger.info("Deleting agent with ID: {}", id);
        
        Agent agent = agentRepository.findById(id)
            .orElseThrow(() -> new AgentNotFoundException("Agent with ID " + id + " not found"));

        // Don't delete busy agents
        if (agent.getStatus() == AgentStatus.BUSY) {
            throw new IllegalStateException("Cannot delete busy agent. Wait for current command to complete.");
        }

        agentRepository.delete(agent);
        logger.info("Successfully deleted agent with ID: {}", id);
    }

    // Command Execution

    /**
     * Executes a command on the specified agent.
     * 
     * @param agentId the agent ID
     * @param commandRequest the command to execute
     * @return the command execution result
     * @throws AgentNotFoundException if agent not found
     * @throws IllegalStateException if agent is not available
     */
    public CommandResult executeCommand(Long agentId, CommandRequest commandRequest) {
        logger.info("Executing command on agent {}: {}", agentId, commandRequest.getCommand());
        
        Agent agent = agentRepository.findById(agentId)
            .orElseThrow(() -> new AgentNotFoundException("Agent with ID " + agentId + " not found"));

        // Check if agent is available
        if (!agent.isAvailable()) {
            throw new IllegalStateException("Agent is not available. Current status: " + agent.getStatus());
        }

        // Validate command
        if (!commandProcessor.isValidCommand(commandRequest.getCommand())) {
            throw new IllegalArgumentException("Invalid command format or content");
        }

        // Create command record
        Command command = new Command(agent, commandRequest.getCommand(), commandRequest.getContext());
        String nlpProvider = commandRequest.getNlpProvider() != null ? 
            commandRequest.getNlpProvider() : defaultNlpProvider;
        command.setNlpProvider(nlpProvider);
        
        // Mark agent as busy and command as executing
        agent.markBusy();
        command.markExecuting();
        
        // Save initial state
        agentRepository.save(agent);
        Command savedCommand = commandRepository.save(command);

        try {
            // Process the command
            CommandResult result = commandProcessor.processCommand(
                commandRequest.getCommand(), 
                commandRequest.getContext(),
                nlpProvider
            );

            // Update command with result
            command.markCompleted(result.getResult(), result.getExecutionTimeMs());
            result.setCommandId(savedCommand.getId());
            
            commandRepository.save(command);
            
            logger.info("Successfully executed command {} on agent {}", savedCommand.getId(), agentId);
            return result;
            
        } catch (Exception e) {
            // Handle execution failure
            command.markFailed(e.getMessage(), System.currentTimeMillis() - savedCommand.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            commandRepository.save(command);
            
            logger.error("Command execution failed for agent {}: {}", agentId, e.getMessage());
            throw new CommandExecutionException("Command execution failed: " + e.getMessage(), e);
            
        } finally {
            // Always mark agent as idle when done
            agent.markIdle();
            agentRepository.save(agent);
        }
    }

    // Query Methods

    /**
     * Finds agents by status.
     * 
     * @param status the agent status
     * @return list of agents with the specified status
     */
    @Transactional(readOnly = true)
    public List<AgentDto> getAgentsByStatus(AgentStatus status) {
        logger.debug("Finding agents by status: {}", status);
        
        List<Agent> agents = agentRepository.findByStatus(status);
        return agents.stream().map(this::mapToDto).toList();
    }

    /**
     * Finds agents by type.
     * 
     * @param type the agent type
     * @return list of agents of the specified type
     */
    @Transactional(readOnly = true)
    public List<AgentDto> getAgentsByType(AgentType type) {
        logger.debug("Finding agents by type: {}", type);
        
        List<Agent> agents = agentRepository.findByType(type);
        return agents.stream().map(this::mapToDto).toList();
    }

    /**
     * Gets all available agents (status = IDLE).
     * 
     * @return list of available agents
     */
    @Transactional(readOnly = true)
    public List<AgentDto> getAvailableAgents() {
        logger.debug("Finding available agents");
        
        List<Agent> agents = agentRepository.findAvailableAgents();
        return agents.stream().map(this::mapToDto).toList();
    }

    /**
     * Gets command history for an agent.
     * 
     * @param agentId the agent ID
     * @param pageable pagination information
     * @return page of commands for the agent
     */
    @Transactional(readOnly = true)
    public Page<Command> getAgentCommandHistory(Long agentId, Pageable pageable) {
        logger.debug("Retrieving command history for agent: {}", agentId);
        
        Agent agent = agentRepository.findById(agentId)
            .orElseThrow(() -> new AgentNotFoundException("Agent with ID " + agentId + " not found"));
        
        return commandRepository.findByAgent(agent, pageable);
    }

    // Statistics Methods

    /**
     * Gets agent statistics including command counts.
     * 
     * @param agentId the agent ID
     * @return agent statistics
     */
    @Transactional(readOnly = true)
    public AgentStatistics getAgentStatistics(Long agentId) {
        logger.debug("Retrieving statistics for agent: {}", agentId);
        
        Agent agent = agentRepository.findById(agentId)
            .orElseThrow(() -> new AgentNotFoundException("Agent with ID " + agentId + " not found"));

        long totalCommands = commandRepository.countByAgent(agent);
        long successfulCommands = commandRepository.findByAgentAndStatus(agent, CommandStatus.COMPLETED).size();
        long failedCommands = commandRepository.findByAgentAndStatus(agent, CommandStatus.FAILED).size();

        return new AgentStatistics(agentId, totalCommands, successfulCommands, failedCommands);
    }

    // Utility Methods

    /**
     * Maps an Agent entity to AgentDto.
     */
    private AgentDto mapToDto(Agent agent) {
        AgentDto dto = new AgentDto();
        dto.setId(agent.getId());
        dto.setName(agent.getName());
        dto.setType(agent.getType());
        dto.setStatus(agent.getStatus());
        dto.setDescription(agent.getDescription());
        dto.setConfiguration(agent.getConfiguration());
        dto.setCommandCount(agent.getCommandCount());
        dto.setCreatedAt(agent.getCreatedAt());
        dto.setUpdatedAt(agent.getUpdatedAt());
        return dto;
    }

    /**
     * Maps an AgentDto to Agent entity.
     */
    private Agent mapToEntity(AgentDto dto) {
        Agent agent = new Agent();
        agent.setName(dto.getName());
        agent.setType(dto.getType());
        agent.setDescription(dto.getDescription());
        agent.setConfiguration(dto.getConfiguration());
        return agent;
    }

    // Inner class for statistics
    public static class AgentStatistics {
        private final Long agentId;
        private final long totalCommands;
        private final long successfulCommands;
        private final long failedCommands;

        public AgentStatistics(Long agentId, long totalCommands, long successfulCommands, long failedCommands) {
            this.agentId = agentId;
            this.totalCommands = totalCommands;
            this.successfulCommands = successfulCommands;
            this.failedCommands = failedCommands;
        }

        public Long getAgentId() { return agentId; }
        public long getTotalCommands() { return totalCommands; }
        public long getSuccessfulCommands() { return successfulCommands; }
        public long getFailedCommands() { return failedCommands; }
        public double getSuccessRate() { 
            return totalCommands > 0 ? (double) successfulCommands / totalCommands : 0.0; 
        }
    }
}

// Custom Exceptions
class AgentNotFoundException extends RuntimeException {
    public AgentNotFoundException(String message) {
        super(message);
    }
}

class CommandExecutionException extends RuntimeException {
    public CommandExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}