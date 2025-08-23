package com.jamesokeeffe.agentsystem.phase2.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for JWT utility functionality.
 * 
 * @author James O'Keeffe
 * @version 2.0.0
 * @since 2.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
class JwtUtilTest {

    @Test
    void testJwtTokenGeneration() {
        JwtUtil jwtUtil = new JwtUtil();
        
        // Test basic token generation
        String username = "testuser";
        String token = jwtUtil.generateToken(username, java.util.Map.of());
        
        assertNotNull(token);
        assertTrue(token.length() > 0);
        
        // Test username extraction
        String extractedUsername = jwtUtil.extractUsername(token);
        assertEquals(username, extractedUsername);
        
        // Test token validation
        assertTrue(jwtUtil.validateToken(token));
    }
}