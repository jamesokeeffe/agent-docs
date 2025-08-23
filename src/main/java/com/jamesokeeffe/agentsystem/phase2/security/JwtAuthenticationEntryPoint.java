package com.jamesokeeffe.agentsystem.phase2.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT Authentication Entry Point for handling unauthorized access.
 * 
 * This component handles authentication failures and returns
 * appropriate HTTP responses for unauthorized requests.
 * 
 * @author James O'Keeffe
 * @version 2.0.0
 * @since 2.0.0
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        
        logger.error("Unauthorized error: {}", authException.getMessage());
        
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        String jsonResponse = """
            {
                "success": false,
                "error": {
                    "code": "UNAUTHORIZED",
                    "message": "Authentication required. Please provide a valid JWT token.",
                    "details": {
                        "path": "%s",
                        "method": "%s",
                        "timestamp": "%s"
                    }
                },
                "data": null,
                "timestamp": "%s",
                "version": "2.0.0"
            }
            """.formatted(
                request.getRequestURI(),
                request.getMethod(),
                java.time.Instant.now().toString(),
                java.time.Instant.now().toString()
            );
        
        response.getWriter().write(jsonResponse);
    }
}