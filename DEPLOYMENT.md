# Deployment Guide - Production Ready

## Deployment Options

### Option 1: Heroku (Easy, Free Tier Available)

#### Prerequisites
- Heroku account
- Heroku CLI installed

#### Steps

1. **Create Heroku app**
   ```bash
   heroku create corporate-kb-app
   ```

2. **Add PostgreSQL with pgvector**
   ```bash
   # Add Heroku Postgres
   heroku addons:create heroku-postgresql:mini
   
   # Enable pgvector
   heroku pg:psql -c "CREATE EXTENSION vector;"
   ```

3. **Set environment variables**
   ```bash
   heroku config:set GEMINI_API_KEY=your_key_here
   ```

4. **Create Procfile**
   ```
   web: java -Dserver.port=$PORT -jar target/corporate-knowledge-base-1.0.0.jar
   ```

5. **Deploy**
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git push heroku main
   ```

### Option 2: Azure App Service

#### Prerequisites
- Azure subscription
- Azure CLI installed

#### Steps

1. **Create resource group**
   ```bash
   az group create --name corporate-kb-rg --location eastus
   ```

2. **Create Azure Database for PostgreSQL**
   ```bash
   az postgres flexible-server create \
     --resource-group corporate-kb-rg \
     --name corporate-kb-db \
     --location eastus \
     --admin-user adminuser \
     --admin-password YourPassword123! \
     --sku-name Standard_B1ms \
     --tier Burstable \
     --version 14
   
   # Enable pgvector (requires PostgreSQL 14+)
   az postgres flexible-server parameter set \
     --resource-group corporate-kb-rg \
     --server-name corporate-kb-db \
     --name azure.extensions --value VECTOR
   ```

3. **Create App Service**
   ```bash
   az appservice plan create \
     --name corporate-kb-plan \
     --resource-group corporate-kb-rg \
     --sku B1 \
     --is-linux
   
   az webapp create \
     --resource-group corporate-kb-rg \
     --plan corporate-kb-plan \
     --name corporate-kb-app \
     --runtime "JAVA:17-java17"
   ```

4. **Configure environment variables**
   ```bash
   az webapp config appsettings set \
     --resource-group corporate-kb-rg \
     --name corporate-kb-app \
     --settings \
       SPRING_DATASOURCE_URL="jdbc:postgresql://corporate-kb-db.postgres.database.azure.com:5432/postgres?sslmode=require" \
       SPRING_DATASOURCE_USERNAME="adminuser" \
       SPRING_DATASOURCE_PASSWORD="YourPassword123!" \
       GEMINI_API_KEY="your_gemini_key"
   ```

5. **Deploy**
   ```bash
   mvn clean package
   az webapp deploy \
     --resource-group corporate-kb-rg \
     --name corporate-kb-app \
     --src-path target/corporate-knowledge-base-1.0.0.jar \
     --type jar
   ```

### Option 3: AWS Elastic Beanstalk

#### Prerequisites
- AWS account
- EB CLI installed

#### Steps

1. **Initialize EB**
   ```bash
   eb init -p java-17 corporate-kb
   ```

2. **Create RDS PostgreSQL instance**
   ```bash
   eb create corporate-kb-env \
     --database \
     --database.engine postgres \
     --database.version 14.7
   ```

3. **Enable pgvector**
   ```bash
   # Connect to RDS
   psql -h your-rds-endpoint -U username -d ebdb
   
   # Enable extension
   CREATE EXTENSION vector;
   ```

4. **Configure environment**
   ```bash
   eb setenv \
     SPRING_DATASOURCE_URL=jdbc:postgresql://your-rds-endpoint:5432/ebdb \
     SPRING_DATASOURCE_USERNAME=username \
     SPRING_DATASOURCE_PASSWORD=password \
     GEMINI_API_KEY=your_key
   ```

5. **Deploy**
   ```bash
   mvn clean package
   eb deploy
   ```

### Option 4: Docker + Cloud Run (Google Cloud)

#### Prerequisites
- Google Cloud account
- gcloud CLI installed

#### Steps

1. **Create Dockerfile**
   ```dockerfile
   FROM eclipse-temurin:17-jdk-alpine
   VOLUME /tmp
   COPY target/corporate-knowledge-base-1.0.0.jar app.jar
   ENTRYPOINT ["java","-jar","/app.jar"]
   ```

2. **Build and push container**
   ```bash
   # Build
   mvn clean package
   docker build -t corporate-kb .
   
   # Tag for GCR
   docker tag corporate-kb gcr.io/YOUR-PROJECT/corporate-kb
   
   # Push
   docker push gcr.io/YOUR-PROJECT/corporate-kb
   ```

3. **Create Cloud SQL PostgreSQL**
   ```bash
   gcloud sql instances create corporate-kb-db \
     --database-version=POSTGRES_14 \
     --tier=db-f1-micro \
     --region=us-central1
   
   # Enable pgvector
   gcloud sql connect corporate-kb-db --user=postgres
   CREATE EXTENSION vector;
   ```

4. **Deploy to Cloud Run**
   ```bash
   gcloud run deploy corporate-kb \
     --image gcr.io/YOUR-PROJECT/corporate-kb \
     --platform managed \
     --region us-central1 \
     --allow-unauthenticated \
     --add-cloudsql-instances YOUR-PROJECT:us-central1:corporate-kb-db \
     --set-env-vars SPRING_DATASOURCE_URL=jdbc:postgresql:///postgres?cloudSqlInstance=YOUR-PROJECT:us-central1:corporate-kb-db&socketFactory=com.google.cloud.sql.postgres.SocketFactory \
     --set-env-vars GEMINI_API_KEY=your_key
   ```

## Production Configuration

### application-prod.properties

```properties
# Database Connection Pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# JPA Production Settings
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

# Logging
logging.level.com.infosys.ai=INFO
logging.level.org.hibernate=WARN
logging.file.name=logs/application.log
logging.file.max-size=10MB
logging.file.max-history=30

# Security
server.error.include-message=never
server.error.include-stacktrace=never

# Performance
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB

# Actuator for monitoring
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
```

## Security Enhancements

### 1. Add Spring Security

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
    <version>0.9.1</version>
</dependency>
```

### 2. Implement JWT Authentication

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/query/**").authenticated()
                .requestMatchers("/api/documents/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            )
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        
        return http.build();
    }
}
```

### 3. Rate Limiting

```java
@Component
public class RateLimitInterceptor extends HandlerInterceptorAdapter {
    
    private final RateLimiter rateLimiter = RateLimiter.create(10.0); // 10 requests per second
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        if (!rateLimiter.tryAcquire()) {
            response.setStatus(429); // Too Many Requests
            return false;
        }
        return true;
    }
}
```

## Performance Optimization

### 1. Add Redis Caching

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(factory)
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)))
            .build();
    }
}

@Service
public class RAGService {
    
    @Cacheable(value = "queries", key = "#request.question")
    public QueryResponse processQuery(QueryRequest request) {
        // ... existing code
    }
}
```

### 2. Async Processing

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}

@Service
public class DocumentService {
    
    @Async
    public CompletableFuture<Document> uploadDocumentAsync(MultipartFile file) {
        Document doc = uploadDocument(file);
        return CompletableFuture.completedFuture(doc);
    }
}
```

## Monitoring & Observability

### 1. Add Spring Boot Actuator

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 2. Prometheus Metrics

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```properties
management.metrics.export.prometheus.enabled=true
management.endpoints.web.exposure.include=prometheus,health,info
```

### 3. Application Insights (Azure)

```xml
<dependency>
    <groupId>com.microsoft.azure</groupId>
    <artifactId>applicationinsights-spring-boot-starter</artifactId>
    <version>3.4.0</version>
</dependency>
```

## CI/CD Pipeline

### GitHub Actions

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [ main ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v2
    
    - name: Set up JDK 17
      uses: actions/setup-java@v2
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Build with Maven
      run: mvn clean package -DskipTests
    
    - name: Run tests
      run: mvn test
    
    - name: Deploy to Heroku
      uses: akhileshns/heroku-deploy@v3.12.12
      with:
        heroku_api_key: ${{secrets.HEROKU_API_KEY}}
        heroku_app_name: "corporate-kb-app"
        heroku_email: "your-email@example.com"
```

## SSL/HTTPS Configuration

### Let's Encrypt with Certbot

```bash
# Install Certbot
sudo apt-get install certbot

# Generate certificate
sudo certbot certonly --standalone -d yourdomain.com

# Configure Spring Boot
server.port=443
server.ssl.key-store=/etc/letsencrypt/live/yourdomain.com/keystore.p12
server.ssl.key-store-password=your_password
server.ssl.key-store-type=PKCS12
```

## Backup Strategy

```bash
# Automated daily backups
#!/bin/bash
# backup.sh

DATE=$(date +%Y%m%d_%H%M%S)
pg_dump -h your-db-host -U username -d corporate_kb > backup_$DATE.sql
aws s3 cp backup_$DATE.sql s3://your-backup-bucket/

# Cron job (daily at 2 AM)
# crontab -e
# 0 2 * * * /path/to/backup.sh
```

## Health Checks

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // Check Gemini API
        // Check Database
        // Check Disk Space
        
        return Health.up()
            .withDetail("gemini", "UP")
            .withDetail("database", "UP")
            .build();
    }
}
```

## Cost Optimization

### Gemini API Usage
- Free Tier: 1500 requests/day
- Monitor usage in Google Cloud Console
- Implement caching to reduce API calls

### Database
- Use connection pooling
- Implement query optimization
- Archive old documents

### Compute
- Use auto-scaling
- Schedule scale-down during off-hours
- Use spot instances where possible

---

**Production Checklist:**
- [ ] Environment variables secured
- [ ] SSL certificate configured
- [ ] Database backups automated
- [ ] Monitoring/alerting set up
- [ ] Rate limiting implemented
- [ ] CORS properly configured
- [ ] Error handling comprehensive
- [ ] Logging structured and centralized
- [ ] Health checks enabled
- [ ] CI/CD pipeline tested
