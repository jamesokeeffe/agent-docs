# Phase 4 Deployment Guide

## Overview

This guide covers deploying the complete multi-agent system to production environments with comprehensive monitoring and observability.

## Prerequisites

- Docker and Docker Compose installed
- At least 8GB RAM available
- 50GB free disk space
- Network access for downloading models

## Quick Start

### 1. Clone and Setup

```bash
git clone <your-repo>
cd agent-docs

# Copy environment configuration
cp .env.example .env

# Edit configuration
nano .env
```

### 2. Environment Configuration

```bash
# .env file
SPRING_PROFILES_ACTIVE=docker
DB_PATH=/data/agentdb
OLLAMA_BASE_URL=http://ollama:11434
JWT_SECRET=your-super-secret-jwt-key-change-this
ADMIN_PASSWORD=your-secure-admin-password

# Monitoring
PROMETHEUS_RETENTION=15d
GRAFANA_ADMIN_PASSWORD=secure-grafana-password

# Resource limits
JAVA_OPTS=-Xmx2g -Xms1g
OLLAMA_NUM_PARALLEL=2
OLLAMA_MAX_LOADED_MODELS=2
```

### 3. Deploy with Docker Compose

```bash
# Start all services
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f agent-system
```

### 4. Initial Setup

```bash
# Download initial models (optional)
docker exec -it agent-docs_ollama_1 ollama pull llama2
docker exec -it agent-docs_ollama_1 ollama pull codellama

# Create admin user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "your-secure-admin-password",
    "role": "ADMIN"
  }'
```

## Service URLs

- **Main Application**: http://localhost:8080
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/your-grafana-password)
- **Ollama API**: http://localhost:11434

## Health Checks

### Application Health
```bash
curl http://localhost:8080/actuator/health
```

### Service Dependencies
```bash
# Check Ollama
curl http://localhost:11434/api/tags

# Check Prometheus
curl http://localhost:9090/-/healthy

# Check Grafana
curl http://localhost:3000/api/health
```

## Monitoring Setup

### 1. Grafana Dashboard Import

1. Access Grafana at http://localhost:3000
2. Login with admin credentials
3. Go to "+" → Import
4. Use dashboard ID `1860` for Node Exporter (if added)
5. Import the custom Agent System dashboard from `grafana/dashboards/agent-system.json`

### 2. Alert Configuration

```yaml
# alertmanager.yml
global:
  smtp_smarthost: 'localhost:587'
  smtp_from: 'alerts@yourdomain.com'

route:
  group_by: ['alertname']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'web.hook'

receivers:
- name: 'web.hook'
  email_configs:
  - to: 'admin@yourdomain.com'
    subject: 'Agent System Alert'
    body: |
      {{ range .Alerts }}
      Alert: {{ .Annotations.summary }}
      Description: {{ .Annotations.description }}
      {{ end }}
```

### 3. Custom Metrics

Monitor these key metrics:

- `agent_requests_total` - Total agent requests
- `agent_response_duration` - Response time percentiles
- `llm_requests_total` - LLM usage
- `workflow_execution_duration` - Workflow performance
- `plugin_executions_total` - Plugin usage
- `jvm_memory_used_bytes` - Memory usage
- `system_cpu_usage` - CPU utilization

## Performance Tuning

### 1. JVM Configuration

```bash
# Adjust based on available memory
JAVA_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### 2. Database Optimization

```yaml
# application-docker.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  jpa:
    properties:
      hibernate:
        cache:
          use_second_level_cache: true
          region:
            factory_class: org.hibernate.cache.caffeine.CaffeineCacheRegionFactory
```

### 3. Ollama Optimization

```bash
# Increase model concurrency for better throughput
docker-compose exec ollama ollama serve --parallel 4
```

## Security Hardening

### 1. Network Security

```yaml
# docker-compose.override.yml
version: '3.8'

services:
  agent-system:
    networks:
      - internal
      - external
    ports:
      - "127.0.0.1:8080:8080"  # Bind to localhost only

  ollama:
    networks:
      - internal
    # Remove external port exposure

  prometheus:
    networks:
      - internal
    # Remove external port exposure

networks:
  internal:
    internal: true
  external:
```

### 2. SSL/TLS Configuration

```nginx
# nginx.conf
server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    ssl_certificate /etc/ssl/certs/your-cert.pem;
    ssl_certificate_key /etc/ssl/private/your-key.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 3. Environment Secrets

```bash
# Use Docker secrets for sensitive data
echo "your-jwt-secret" | docker secret create jwt_secret -
echo "your-db-password" | docker secret create db_password -
```

## Backup and Recovery

### 1. Data Backup

```bash
#!/bin/bash
# backup.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups/agent-system"

# Create backup directory
mkdir -p $BACKUP_DIR

# Backup database
docker exec agent-docs_agent-system_1 \
  sh -c 'cd /data && tar czf - agentdb*' > $BACKUP_DIR/database_$DATE.tar.gz

# Backup configuration
docker exec agent-docs_agent-system_1 \
  sh -c 'cd /app && tar czf - config/' > $BACKUP_DIR/config_$DATE.tar.gz

# Backup Ollama models
docker exec agent-docs_ollama_1 \
  sh -c 'cd /root/.ollama && tar czf - .' > $BACKUP_DIR/ollama_$DATE.tar.gz

echo "Backup completed: $BACKUP_DIR"
```

### 2. Recovery Process

```bash
#!/bin/bash
# restore.sh

BACKUP_FILE=$1

if [ -z "$BACKUP_FILE" ]; then
    echo "Usage: $0 <backup_file>"
    exit 1
fi

# Stop services
docker-compose down

# Restore database
tar xzf $BACKUP_FILE/database_*.tar.gz -C /var/lib/docker/volumes/agent-docs_agent-data/_data/

# Restore configuration
tar xzf $BACKUP_FILE/config_*.tar.gz -C ./

# Restore Ollama models
tar xzf $BACKUP_FILE/ollama_*.tar.gz -C /var/lib/docker/volumes/agent-docs_ollama-data/_data/

# Start services
docker-compose up -d

echo "Recovery completed"
```

## Scaling Considerations

### 1. Horizontal Scaling

```yaml
# docker-compose.scale.yml
version: '3.8'

services:
  agent-system:
    deploy:
      replicas: 3
    
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    depends_on:
      - agent-system
```

### 2. Load Balancer Configuration

```nginx
upstream agent_backend {
    server agent-system_1:8080;
    server agent-system_2:8080;
    server agent-system_3:8080;
}

server {
    listen 80;
    location / {
        proxy_pass http://agent_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 3. Database Scaling

For production loads, consider migrating to PostgreSQL:

```yaml
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: agentdb
      POSTGRES_USER: agentuser
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  agent-system:
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/agentdb
      SPRING_DATASOURCE_USERNAME: agentuser
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_JPA_DATABASE_PLATFORM: org.hibernate.dialect.PostgreSQLDialect
```

## Troubleshooting

### Common Issues

#### 1. Out of Memory Errors
```bash
# Check memory usage
docker stats

# Increase JVM heap
docker-compose exec agent-system \
  sh -c 'export JAVA_OPTS="-Xmx4g" && java -jar app.jar'
```

#### 2. Ollama Connection Issues
```bash
# Check Ollama status
docker-compose exec ollama ollama list

# Restart Ollama service
docker-compose restart ollama

# Check logs
docker-compose logs ollama
```

#### 3. Database Connection Problems
```bash
# Check database file permissions
docker-compose exec agent-system ls -la /data/

# Recreate database volume
docker-compose down
docker volume rm agent-docs_agent-data
docker-compose up -d
```

#### 4. High CPU Usage
```bash
# Monitor JVM threads
docker-compose exec agent-system jstack 1

# Check for memory leaks
docker-compose exec agent-system jstat -gc 1 5s
```

### Log Analysis

```bash
# Follow application logs
docker-compose logs -f agent-system

# Search for errors
docker-compose logs agent-system | grep ERROR

# Export logs for analysis
docker-compose logs --since 1h agent-system > agent-system.log
```

## Maintenance

### Regular Tasks

#### Daily
- Check service health status
- Monitor resource usage
- Review error logs

#### Weekly
- Update security patches
- Clean old log files
- Backup database

#### Monthly
- Update base images
- Review and optimize queries
- Update Ollama models

### Automated Maintenance

```bash
#!/bin/bash
# maintenance.sh

# Clean old Docker images
docker image prune -f

# Clean old logs
find /var/log -name "*.log" -mtime +30 -delete

# Update images
docker-compose pull
docker-compose up -d

# Health check
curl -f http://localhost:8080/actuator/health || exit 1

echo "Maintenance completed successfully"
```

## Support and Monitoring

### Key Metrics to Monitor

1. **Application Metrics**
   - Response times < 2 seconds
   - Error rate < 1%
   - Memory usage < 80%
   - CPU usage < 70%

2. **LLM Metrics**
   - Model availability
   - Generation speed
   - Queue lengths
   - Error rates

3. **System Metrics**
   - Disk usage < 85%
   - Network connectivity
   - Container health
   - Database performance

### Alert Thresholds

```yaml
# prometheus-alerts.yml
groups:
- name: agent-system
  rules:
  - alert: HighErrorRate
    expr: rate(agent_requests_total{status="error"}[5m]) > 0.1
    for: 2m
    annotations:
      summary: High error rate detected

  - alert: HighMemoryUsage
    expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.8
    for: 5m
    annotations:
      summary: High memory usage

  - alert: SlowResponse
    expr: histogram_quantile(0.95, rate(agent_response_duration_bucket[5m])) > 5
    for: 2m
    annotations:
      summary: Slow response times
```

This deployment guide provides a comprehensive foundation for running the multi-agent system in production with proper monitoring, security, and maintenance practices.