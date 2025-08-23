package com.jamesokeeffe.agentsystem.repository;

import com.jamesokeeffe.agentsystem.model.Agent;
import com.jamesokeeffe.agentsystem.model.AgentStatus;
import com.jamesokeeffe.agentsystem.model.AgentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Agent entity operations.
 * 
 * Provides CRUD operations and custom queries for agent management including:
 * - Finding agents by various criteria
 * - Status-based queries
 * - Performance and statistics queries
 * 
 * @author James O'Keeffe
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    /**
     * Finds an agent by its unique name.
     * 
     * @param name the agent name
     * @return optional containing the agent if found
     */
    Optional<Agent> findByName(String name);

    /**
     * Finds all agents with the specified status.
     * 
     * @param status the agent status to filter by
     * @return list of agents with the specified status
     */
    List<Agent> findByStatus(AgentStatus status);

    /**
     * Finds all agents of the specified type.
     * 
     * @param type the agent type to filter by
     * @return list of agents of the specified type
     */
    List<Agent> findByType(AgentType type);

    /**
     * Finds agents by type and status combination.
     * 
     * @param type the agent type
     * @param status the agent status
     * @return list of agents matching both criteria
     */
    List<Agent> findByTypeAndStatus(AgentType type, AgentStatus status);

    /**
     * Finds all available (IDLE status) agents.
     * 
     * @return list of available agents
     */
    @Query("SELECT a FROM Agent a WHERE a.status = 'IDLE'")
    List<Agent> findAvailableAgents();

    /**
     * Finds agents created within a specific time range.
     * 
     * @param start the start date/time
     * @param end the end date/time
     * @return list of agents created in the time range
     */
    List<Agent> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Counts agents by status.
     * 
     * @param status the agent status
     * @return count of agents with the specified status
     */
    long countByStatus(AgentStatus status);

    /**
     * Counts agents by type.
     * 
     * @param type the agent type
     * @return count of agents of the specified type
     */
    long countByType(AgentType type);

    /**
     * Checks if an agent with the given name exists.
     * 
     * @param name the agent name
     * @return true if an agent with this name exists
     */
    boolean existsByName(String name);

    /**
     * Finds agents that have executed commands recently.
     * 
     * @param since the date/time threshold
     * @return list of recently active agents
     */
    @Query("SELECT DISTINCT a FROM Agent a JOIN a.commands c WHERE c.timestamp >= :since")
    List<Agent> findActiveAgentsSince(@Param("since") LocalDateTime since);

    /**
     * Gets agent statistics including command counts.
     * 
     * @return list of agents with their command counts
     */
    @Query("SELECT a FROM Agent a LEFT JOIN FETCH a.commands")
    List<Agent> findAllWithCommandCounts();

    /**
     * Finds the most active agents based on command execution count.
     * 
     * @param limit the maximum number of agents to return
     * @return list of most active agents
     */
    @Query(value = """
        SELECT a.* FROM agents a 
        LEFT JOIN commands c ON a.id = c.agent_id 
        GROUP BY a.id 
        ORDER BY COUNT(c.id) DESC 
        LIMIT :limit
        """, nativeQuery = true)
    List<Agent> findMostActiveAgents(@Param("limit") int limit);

    /**
     * Finds agents that haven't executed any commands recently.
     * 
     * @param threshold the inactivity threshold
     * @return list of inactive agents
     */
    @Query("""
        SELECT a FROM Agent a 
        WHERE a.id NOT IN (
            SELECT DISTINCT c.agent.id FROM Command c 
            WHERE c.timestamp >= :threshold
        )
        """)
    List<Agent> findInactiveAgentsSince(@Param("threshold") LocalDateTime threshold);

    /**
     * Custom query to find agents with specific configuration properties.
     * 
     * @param configKey the configuration key to search for
     * @param configValue the configuration value to match
     * @return list of agents with matching configuration
     */
    @Query("SELECT a FROM Agent a WHERE a.configuration LIKE %:configKey% AND a.configuration LIKE %:configValue%")
    List<Agent> findByConfigurationContaining(@Param("configKey") String configKey, @Param("configValue") String configValue);
}