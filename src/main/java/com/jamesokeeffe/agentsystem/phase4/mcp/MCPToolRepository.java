package com.jamesokeeffe.agentsystem.phase4.mcp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MCPTool entity operations.
 * 
 * @author James O'Keeffe
 * @version 4.0.0
 * @since 4.0.0
 */
@Repository
public interface MCPToolRepository extends JpaRepository<MCPTool, Long> {

    Optional<MCPTool> findByName(String name);
    
    Optional<MCPTool> findByNameAndEnabledTrue(String name);
    
    List<MCPTool> findByEnabledTrue();
    
    List<MCPTool> findByType(MCPToolType type);
    
    List<MCPTool> findByAgentId(Long agentId);
    
    @Query("SELECT t FROM MCPTool t WHERE t.usageCount > :threshold")
    List<MCPTool> findPopularTools(@Param("threshold") int threshold);
    
    @Query("SELECT t FROM MCPTool t WHERE t.lastUsedAt > :date")
    List<MCPTool> findRecentlyUsedTools(@Param("date") LocalDateTime date);
}