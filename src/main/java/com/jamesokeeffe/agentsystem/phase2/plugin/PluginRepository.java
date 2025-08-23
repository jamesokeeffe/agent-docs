package com.jamesokeeffe.agentsystem.phase2.plugin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Plugin entity operations.
 * 
 * Provides data access methods for plugin management:
 * - Basic CRUD operations
 * - Queries by agent and status
 * - Plugin ordering and filtering
 * 
 * @author James O'Keeffe
 * @version 2.0.0
 * @since 2.0.0
 */
@Repository
public interface PluginRepository extends JpaRepository<Plugin, Long> {

    /**
     * Find all plugins for a specific agent.
     */
    List<Plugin> findByAgentId(Long agentId);

    /**
     * Find enabled plugins for a specific agent.
     */
    List<Plugin> findByAgentIdAndEnabledTrue(Long agentId);

    /**
     * Find enabled plugins for a specific agent ordered by load priority.
     */
    List<Plugin> findByAgentIdAndEnabledTrueOrderByLoadPriorityDesc(Long agentId);

    /**
     * Find plugins by agent and type.
     */
    List<Plugin> findByAgentIdAndType(Long agentId, PluginType type);

    /**
     * Find plugins by agent and status.
     */
    List<Plugin> findByAgentIdAndStatus(Long agentId, PluginStatus status);

    /**
     * Find plugin by agent and name.
     */
    Optional<Plugin> findByAgentIdAndName(Long agentId, String name);

    /**
     * Find active plugins for an agent.
     */
    @Query("SELECT p FROM Plugin p WHERE p.agentId = :agentId AND p.enabled = true AND p.status = 'ACTIVE'")
    List<Plugin> findActivePluginsByAgent(@Param("agentId") Long agentId);

    /**
     * Count enabled plugins for an agent.
     */
    @Query("SELECT COUNT(p) FROM Plugin p WHERE p.agentId = :agentId AND p.enabled = true")
    Long countEnabledPluginsByAgent(@Param("agentId") Long agentId);

    /**
     * Find plugins by type across all agents.
     */
    List<Plugin> findByType(PluginType type);

    /**
     * Find plugins by status across all agents.
     */
    List<Plugin> findByStatus(PluginStatus status);

    /**
     * Check if a plugin name exists for an agent.
     */
    boolean existsByAgentIdAndName(Long agentId, String name);

    /**
     * Find plugins that need to be loaded (installed but not active).
     */
    @Query("SELECT p FROM Plugin p WHERE p.enabled = true AND p.status = 'INSTALLED'")
    List<Plugin> findPluginsToLoad();
}