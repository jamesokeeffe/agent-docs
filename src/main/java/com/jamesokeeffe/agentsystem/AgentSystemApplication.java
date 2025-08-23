package com.jamesokeeffe.agentsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for the Multi-Agent System.
 * 
 * This Spring Boot application provides a comprehensive multi-agent system
 * that evolves through multiple phases:
 * 
 * Phase 1: Foundation - Basic autonomous agent with REST API
 * Phase 2: Enhanced Capabilities - AI integration and plugin system
 * Phase 3: Multi-Agent Orchestration - Agent coordination and workflows
 * Phase 4: MCP Integration - Model Context Protocol and production features
 * 
 * @author James O'Keeffe
 * @version 1.0.0
 * @since 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableTransactionManagement
public class AgentSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentSystemApplication.class, args);
    }
}