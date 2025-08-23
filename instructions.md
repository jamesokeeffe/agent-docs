# instructions.md

## Copilot Agent Instructions for Repository Interaction

### Core Development Principles

#### Clean Code & SOLID Principles
- **Single Responsibility Principle**: Each class should have one reason to change
- **Open/Closed Principle**: Software entities should be open for extension, closed for modification
- **Liskov Substitution Principle**: Objects should be replaceable with instances of their subtypes
- **Interface Segregation Principle**: Many client-specific interfaces are better than one general-purpose interface
- **Dependency Inversion Principle**: Depend on abstractions, not concretions

#### Code Quality Standards
- **KISS (Keep It Simple, Stupid)**: Favor simplicity over complexity
- **DRY (Don't Repeat Yourself)**: Eliminate code duplication through abstraction
- **YAGNI (You Aren't Gonna Need It)**: Don't implement features until they're actually needed
- **Fail Fast**: Validate inputs early and throw meaningful exceptions
- **Immutability**: Prefer immutable objects where possible

### Object-Oriented Programming Best Practices

#### Design Patterns
- **Factory Pattern**: For object creation with complex logic
- **Strategy Pattern**: For interchangeable algorithms
- **Observer Pattern**: For event-driven architectures
- **Singleton Pattern**: Only when truly needed (consider dependency injection instead)
- **Builder Pattern**: For complex object construction

#### Class Design
- Favor composition over inheritance
- Use meaningful names for classes, methods, and variables
- Keep methods small and focused (max 20 lines)
- Minimize public interface exposure
- Use dependency injection for loose coupling

### Cost vs Performance AI Optimizations

#### Resource Efficiency
- **Lazy Loading**: Load data only when needed
- **Caching Strategies**: Implement Redis/in-memory caching for frequently accessed data
- **Connection Pooling**: Reuse database and HTTP connections
- **Batch Processing**: Group operations to reduce overhead
- **Asynchronous Processing**: Use non-blocking operations where appropriate

#### Performance Monitoring
- Track key metrics: response time, throughput, resource utilization
- Implement circuit breakers for external service calls
- Use profiling tools to identify bottlenecks
- Monitor memory usage and garbage collection patterns

#### Cost Optimization
- **Auto-scaling**: Scale resources based on demand
- **Reserved Instances**: Use for predictable workloads
- **Spot Instances**: For fault-tolerant batch processing
- **Resource Right-sizing**: Match instance types to workload requirements
- **Data Lifecycle Management**: Archive or delete unused data

### Cloud-First Architecture

#### Microservices Design
- **Service Boundaries**: Align with business capabilities
- **Database per Service**: Each service owns its data
- **API Gateway**: Central entry point for all requests
- **Service Discovery**: Dynamic service registration and discovery
- **Distributed Tracing**: Track requests across services

#### Containerization (Docker)
```dockerfile
# Multi-stage builds for smaller images
FROM maven:3.8-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

FROM openjdk:17-jre-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Kubernetes Deployment
- Use Helm charts for templating
- Implement health checks (liveness, readiness)
- Configure resource limits and requests
- Use ConfigMaps and Secrets for configuration
- Implement horizontal pod autoscaling

#### Cloud Native Patterns
- **12-Factor App**: Follow twelve-factor methodology
- **Circuit Breaker**: Prevent cascade failures
- **Bulkhead**: Isolate critical resources
- **Retry with Backoff**: Handle transient failures
- **Dead Letter Queue**: Handle failed message processing

### Infrastructure as Code

#### Terraform Configuration
```hcl
resource "aws_ecs_cluster" "main" {
  name = "agent-docs-cluster"
  
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

resource "aws_ecs_service" "app" {
  name            = "agent-docs-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 2
  
  deployment_configuration {
    maximum_percent         = 200
    minimum_healthy_percent = 100
  }
}
```

#### CI/CD Pipeline
- **Build**: Automated testing and artifact creation
- **Security Scanning**: Container and dependency vulnerability checks
- **Deployment**: Blue-green or rolling deployments
- **Monitoring**: Post-deployment health checks
- **Rollback**: Automated rollback on failure detection

### Security Best Practices

#### Authentication & Authorization
- Use OAuth 2.0/OpenID Connect for authentication
- Implement role-based access control (RBAC)
- Store secrets in dedicated secret management services
- Use short-lived tokens with refresh mechanisms

#### Data Protection
- Encrypt data at rest and in transit
- Implement input validation and sanitization
- Use parameterized queries to prevent SQL injection
- Regular security audits and penetration testing

### Monitoring & Observability

#### Logging
- Structured logging (JSON format)
- Centralized log aggregation (ELK stack)
- Log correlation with trace IDs
- Appropriate log levels (ERROR, WARN, INFO, DEBUG)

#### Metrics
- Application metrics (business and technical)
- Infrastructure metrics (CPU, memory, disk)
- Custom dashboards for key performance indicators
- Alerting on threshold breaches

#### Distributed Tracing
- Trace requests across all services
- Identify performance bottlenecks
- Debug complex distributed system issues
- Correlate traces with logs and metrics

### Development Workflow

#### Git Best Practices
- Use feature branches for development
- Write descriptive commit messages
- Squash commits before merging
- Use pull request reviews for code quality

#### Testing Strategy
- Unit tests (80% coverage minimum)
- Integration tests for API endpoints
- Contract testing for service boundaries
- End-to-end tests for critical user journeys
- Performance testing for scalability validation

#### Code Review Guidelines
- Focus on logic, not style (use automated formatters)
- Check for security vulnerabilities
- Verify test coverage and quality
- Ensure documentation is updated
- Validate architectural decisions

### Agent-Specific Considerations

#### Multi-Agent Communication
- Use message queues for asynchronous communication
- Implement retry mechanisms for failed communications
- Design for eventual consistency
- Use correlation IDs for message tracking

#### Scalability Patterns
- Horizontal scaling over vertical scaling
- Stateless service design
- Event-driven architecture
- CQRS for read/write separation where appropriate

#### Error Handling
- Graceful degradation when dependencies fail
- Meaningful error messages for debugging
- Proper exception propagation
- Dead letter queues for failed processing

---

**Remember**: Always prioritize maintainability and readability over premature optimization. Measure before optimizing, and optimize for the common case.
