package com.jamesokeeffe.agentsystem.phase4.mcp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MCPResource entity operations.
 * 
 * @author James O'Keeffe
 * @version 4.0.0
 * @since 4.0.0
 */
@Repository
public interface MCPResourceRepository extends JpaRepository<MCPResource, Long> {

    Optional<MCPResource> findByUri(String uri);
    
    List<MCPResource> findByMimeType(String mimeType);
    
    List<MCPResource> findByAccessLevel(MCPAccessLevel accessLevel);
    
    List<MCPResource> findByAgentId(Long agentId);
    
    @Query("SELECT r FROM MCPResource r WHERE r.accessCount > :threshold")
    List<MCPResource> findPopularResources(@Param("threshold") int threshold);
    
    @Query("SELECT r FROM MCPResource r WHERE r.lastAccessedAt > :date")
    List<MCPResource> findRecentlyAccessedResources(@Param("date") LocalDateTime date);
    
    @Query("SELECT r FROM MCPResource r WHERE r.uri LIKE %:pattern%")
    List<MCPResource> findByUriPattern(@Param("pattern") String pattern);
}