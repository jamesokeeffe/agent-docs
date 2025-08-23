# Phase 2 Security Guide: JWT Authentication & Authorization

## Overview

This guide covers implementing JWT-based authentication and role-based authorization in the multi-agent system.

## JWT Authentication Implementation

### 1. Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/agents").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/agents").hasRole("ADMIN")
                .requestMatchers("/api/v1/plugins/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

### 2. JWT Token Service

```java
@Service
public class JwtTokenService {
    
    private static final String SECRET_KEY = "your-secret-key";
    private static final long ACCESS_TOKEN_EXPIRATION = 900000; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000; // 7 days
    
    public String generateAccessToken(UserDetails userDetails) {
        return createToken(userDetails.getUsername(), ACCESS_TOKEN_EXPIRATION);
    }
    
    public String generateRefreshToken(String username) {
        return createToken(username, REFRESH_TOKEN_EXPIRATION);
    }
    
    private String createToken(String username, long expiration) {
        Date expiryDate = new Date(System.currentTimeMillis() + expiration);
        
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
            .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(SECRET_KEY)
            .parseClaimsJws(token)
            .getBody();
        return claims.getSubject();
    }
}
```

### 3. Authentication Controller

```java
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtTokenService jwtTokenService;
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );
            
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String accessToken = jwtTokenService.generateAccessToken(userDetails);
            String refreshToken = jwtTokenService.generateRefreshToken(userDetails.getUsername());
            
            // Store refresh token
            userService.saveRefreshToken(userDetails.getUsername(), refreshToken);
            
            return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900) // 15 minutes
                .build());
                
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponse.builder()
                    .error("Invalid username or password")
                    .build());
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        
        if (!jwtTokenService.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponse.builder()
                    .error("Invalid refresh token")
                    .build());
        }
        
        String username = jwtTokenService.getUsernameFromToken(refreshToken);
        
        if (!userService.isRefreshTokenValid(username, refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponse.builder()
                    .error("Refresh token not found or expired")
                    .build());
        }
        
        UserDetails userDetails = userService.loadUserByUsername(username);
        String newAccessToken = jwtTokenService.generateAccessToken(userDetails);
        
        return ResponseEntity.ok(AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(900)
            .build());
    }
    
    @GetMapping("/validate")
    public ResponseEntity<ValidationResponse> validate(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ValidationResponse.builder()
                    .valid(false)
                    .error("Missing or invalid authorization header")
                    .build());
        }
        
        String token = authHeader.substring(7);
        boolean isValid = jwtTokenService.validateToken(token);
        
        if (isValid) {
            String username = jwtTokenService.getUsernameFromToken(token);
            User user = userService.findByUsername(username);
            
            return ResponseEntity.ok(ValidationResponse.builder()
                .valid(true)
                .username(username)
                .role(user.getRole().name())
                .build());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ValidationResponse.builder()
                    .valid(false)
                    .error("Invalid or expired token")
                    .build());
        }
    }
}
```

## Role-Based Access Control

### 1. User Roles

```java
public enum UserRole {
    USER("USER"),
    ADMIN("ADMIN"),
    AGENT("AGENT");
    
    private final String role;
    
    UserRole(String role) {
        this.role = role;
    }
    
    public String getRole() {
        return role;
    }
}
```

### 2. User Entity

```java
@Entity
@Table(name = "users")
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String passwordHash;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RefreshToken> refreshTokens = new ArrayList<>();
    
    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    
    @Override
    public String getPassword() {
        return passwordHash;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

### 3. Method-Level Security

```java
@Service
@PreAuthorize("hasRole('ADMIN')")
public class PluginService {
    
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
    public List<Plugin> getPluginsForAgent(Long agentId) {
        // Implementation
    }
    
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public PluginResult executePlugin(String pluginName, PluginRequest request) {
        // Implementation
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    public void enablePlugin(String pluginId) {
        // Implementation
    }
}
```

## Security Best Practices

### 1. Password Security

```java
@Service
public class UserService {
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    
    public User createUser(String username, String password, UserRole role) {
        String hashedPassword = passwordEncoder.encode(password);
        
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hashedPassword);
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
    
    public boolean validatePassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
}
```

### 2. Token Security

- **Secure Storage**: Store JWT secrets in environment variables
- **Token Rotation**: Implement refresh token rotation
- **Blacklisting**: Maintain a blacklist for revoked tokens
- **HTTPS Only**: Always use HTTPS in production

### 3. Rate Limiting

```java
@Component
public class RateLimitingFilter implements Filter {
    
    private final Map<String, List<Long>> requestCounts = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS = 100;
    private static final long TIME_WINDOW = 3600000; // 1 hour
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String clientIp = getClientIp(httpRequest);
        
        if (isRateLimited(clientIp)) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    private boolean isRateLimited(String clientIp) {
        long currentTime = System.currentTimeMillis();
        List<Long> requests = requestCounts.computeIfAbsent(clientIp, k -> new ArrayList<>());
        
        // Remove old requests outside time window
        requests.removeIf(time -> currentTime - time > TIME_WINDOW);
        
        if (requests.size() >= MAX_REQUESTS) {
            return true;
        }
        
        requests.add(currentTime);
        return false;
    }
}
```

## Testing Security

### 1. Authentication Tests

```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthControllerTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = LoginRequest.builder()
            .username("admin")
            .password("admin123")
            .build();
        
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/login", request, AuthResponse.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getAccessToken()).isNotNull();
    }
    
    @Test
    void shouldRejectInvalidCredentials() {
        LoginRequest request = LoginRequest.builder()
            .username("admin")
            .password("wrongpassword")
            .build();
        
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/login", request, AuthResponse.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
```

### 2. Authorization Tests

```java
@Test
void shouldRequireAdminRoleForPluginManagement() {
    // Create user token
    String userToken = createTokenForRole(UserRole.USER);
    
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(userToken);
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    
    ResponseEntity<String> response = restTemplate.exchange(
        "/api/v1/plugins/echo/enable", 
        HttpMethod.POST, 
        entity, 
        String.class);
    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
}
```

## Troubleshooting

### Common Issues

1. **Token Expiration**: Implement proper refresh token handling
2. **CORS Issues**: Configure CORS for frontend integration
3. **Role Mismatch**: Ensure role names match between frontend and backend
4. **Database Constraints**: Handle unique constraint violations

### Security Monitoring

```java
@Component
@Slf4j
public class SecurityEventListener {
    
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        log.info("Successful login for user: {}", username);
    }
    
    @EventListener
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        log.warn("Failed login attempt for user: {}", username);
    }
}
```

This security implementation provides a robust foundation for protecting the multi-agent system while maintaining flexibility for different user roles and access patterns.