package com.jamesokeeffe.agentsystem.repository;

import com.jamesokeeffe.agentsystem.model.Agent;
import com.jamesokeeffe.agentsystem.model.Command;
import com.jamesokeeffe.agentsystem.model.CommandStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Command entity operations.
 * 
 * Provides CRUD operations and custom queries for command management including:
 * - Finding commands by agent, status, and time range
 * - Performance and analytics queries
 * - Command history and statistics
 * 
 * @author James O'Keeffe
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface CommandRepository extends JpaRepository<Command, Long> {

    /**
     * Finds all commands executed by a specific agent.
     * 
     * @param agent the agent
     * @param pageable pagination information
     * @return page of commands for the agent
     */
    Page<Command> findByAgent(Agent agent, Pageable pageable);

    /**
     * Finds all commands for a specific agent by agent ID.
     * 
     * @param agentId the agent ID
     * @return list of commands for the agent
     */
    List<Command> findByAgentId(Long agentId);

    /**
     * Finds commands by status.
     * 
     * @param status the command status
     * @return list of commands with the specified status
     */
    List<Command> findByStatus(CommandStatus status);

    /**
     * Finds commands executed within a specific time range.
     * 
     * @param start the start date/time
     * @param end the end date/time
     * @return list of commands in the time range
     */
    List<Command> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Finds commands by agent and status.
     * 
     * @param agent the agent
     * @param status the command status
     * @return list of commands matching both criteria
     */
    List<Command> findByAgentAndStatus(Agent agent, CommandStatus status);

    /**
     * Finds recent commands for an agent.
     * 
     * @param agent the agent
     * @param since the date/time threshold
     * @return list of recent commands
     */
    List<Command> findByAgentAndTimestampAfter(Agent agent, LocalDateTime since);

    /**
     * Counts commands by status.
     * 
     * @param status the command status
     * @return count of commands with the specified status
     */
    long countByStatus(CommandStatus status);

    /**
     * Counts commands for a specific agent.
     * 
     * @param agent the agent
     * @return total command count for the agent
     */
    long countByAgent(Agent agent);

    /**
     * Finds the most recent commands across all agents.
     * 
     * @param limit the maximum number of commands to return
     * @return list of most recent commands
     */
    @Query("SELECT c FROM Command c ORDER BY c.timestamp DESC")
    List<Command> findMostRecentCommands(Pageable pageable);

    /**
     * Finds commands with execution time above a threshold.
     * 
     * @param thresholdMs the execution time threshold in milliseconds
     * @return list of slow commands
     */
    List<Command> findByExecutionTimeMsGreaterThan(Long thresholdMs);

    /**
     * Gets average execution time for successful commands.
     * 
     * @return average execution time in milliseconds
     */
    @Query("SELECT AVG(c.executionTimeMs) FROM Command c WHERE c.status = 'COMPLETED' AND c.executionTimeMs IS NOT NULL")
    Double getAverageExecutionTime();

    /**
     * Gets command statistics by status.
     * 
     * @return list of status counts
     */
    @Query("SELECT c.status, COUNT(c) FROM Command c GROUP BY c.status")
    List<Object[]> getCommandStatsByStatus();

    /**
     * Finds commands containing specific text in content.
     * 
     * @param searchText the text to search for
     * @return list of commands containing the text
     */
    @Query("SELECT c FROM Command c WHERE LOWER(c.content) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    List<Command> findByContentContainingIgnoreCase(@Param("searchText") String searchText);

    /**
     * Gets the most frequent command patterns.
     * 
     * @param limit the maximum number of patterns to return
     * @return list of command patterns with their frequencies
     */
    @Query(value = """
        SELECT SUBSTRING(content, 1, 50) as pattern, COUNT(*) as frequency 
        FROM commands 
        GROUP BY SUBSTRING(content, 1, 50) 
        ORDER BY frequency DESC 
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> getMostFrequentCommandPatterns(@Param("limit") int limit);

    /**
     * Finds failed commands with error messages.
     * 
     * @return list of failed commands
     */
    @Query("SELECT c FROM Command c WHERE c.status = 'FAILED' AND c.errorMessage IS NOT NULL")
    List<Command> findFailedCommandsWithErrors();

    /**
     * Gets hourly command execution statistics.
     * 
     * @param start the start date/time
     * @param end the end date/time
     * @return hourly execution counts
     */
    @Query(value = """
        SELECT DATE_FORMAT(timestamp, '%Y-%m-%d %H:00:00') as hour, 
               COUNT(*) as command_count,
               COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as successful_count,
               COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed_count
        FROM commands 
        WHERE timestamp BETWEEN :start AND :end 
        GROUP BY DATE_FORMAT(timestamp, '%Y-%m-%d %H:00:00') 
        ORDER BY hour
        """, nativeQuery = true)
    List<Object[]> getHourlyCommandStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Finds commands by NLP provider.
     * 
     * @param nlpProvider the NLP provider name
     * @return list of commands processed by the provider
     */
    List<Command> findByNlpProvider(String nlpProvider);

    /**
     * Gets performance metrics for agents based on command execution.
     * 
     * @return agent performance statistics
     */
    @Query("""
        SELECT c.agent.id, c.agent.name, 
               COUNT(c) as totalCommands,
               COUNT(CASE WHEN c.status = 'COMPLETED' THEN 1 END) as successfulCommands,
               AVG(c.executionTimeMs) as avgExecutionTime
        FROM Command c 
        GROUP BY c.agent.id, c.agent.name 
        ORDER BY successfulCommands DESC
        """)
    List<Object[]> getAgentPerformanceStats();
}