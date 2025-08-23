package com.jamesokeeffe.agentsystem.phase2.security;

import com.jamesokeeffe.agentsystem.controller.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication controller for JWT-based authentication.
 * 
 * Provides endpoints for:
 * - User login and token generation
 * - Token refresh
 * - Authentication status validation
 * 
 * @author James O'Keeffe
 * @version 2.0.0
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Authenticates user and returns JWT token.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest authRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    authRequest.getUsername(),
                    authRequest.getPassword()
                )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            AuthResponse authResponse = new AuthResponse(
                token,
                refreshToken,
                userDetails.getUsername(),
                userDetails.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .toArray(String[]::new)
            );

            logger.info("User {} authenticated successfully", authRequest.getUsername());
            return ResponseEntity.ok(ApiResponse.success(authResponse));

        } catch (BadCredentialsException e) {
            logger.warn("Authentication failed for user: {}", authRequest.getUsername());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_CREDENTIALS", "Invalid username or password"));
        }
    }

    /**
     * Validates current token and returns user info.
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            
            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.extractUsername(token);
                String[] roles = jwtUtil.extractRoles(token);
                
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("username", username);
                userInfo.put("roles", roles);
                userInfo.put("valid", true);
                
                return ResponseEntity.ok(ApiResponse.success(userInfo));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_TOKEN", "Token is invalid or expired"));
            }
        } catch (Exception e) {
            logger.error("Token validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("TOKEN_ERROR", "Error validating token"));
        }
    }

    /**
     * Refreshes JWT token using refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshRequest refreshRequest) {
        try {
            String refreshToken = refreshRequest.getRefreshToken();
            
            if (jwtUtil.validateToken(refreshToken)) {
                String username = jwtUtil.extractUsername(refreshToken);
                
                // Generate new tokens
                Map<String, Object> claims = new HashMap<>();
                String newToken = jwtUtil.generateToken(username, claims);
                String newRefreshToken = jwtUtil.generateToken(username, claims);
                
                AuthResponse authResponse = new AuthResponse(
                    newToken,
                    newRefreshToken,
                    username,
                    jwtUtil.extractRoles(refreshToken)
                );
                
                return ResponseEntity.ok(ApiResponse.success(authResponse));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired"));
            }
        } catch (Exception e) {
            logger.error("Token refresh error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("REFRESH_ERROR", "Error refreshing token"));
        }
    }

    // DTOs
    public static class AuthRequest {
        private String username;
        private String password;

        public AuthRequest() {}

        public AuthRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RefreshRequest {
        private String refreshToken;

        public RefreshRequest() {}

        public RefreshRequest(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String username;
        private String[] roles;

        public AuthResponse() {}

        public AuthResponse(String accessToken, String refreshToken, String username, String[] roles) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.username = username;
            this.roles = roles;
        }

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String[] getRoles() { return roles; }
        public void setRoles(String[] roles) { this.roles = roles; }
    }
}