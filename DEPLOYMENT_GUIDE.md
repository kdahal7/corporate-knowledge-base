# Deployment Guide - AI-Powered Corporate Knowledge Base

## Prerequisites for Online Deployment

1. **Java 17 or higher** installed on server
2. **PostgreSQL with pgvector extension** (Cloud database)
3. **Google Gemini API Key** (Already configured: AIzaSyDIRej1JqgHEJRV5N5kgvutY580N9MPRvo)
4. **Maven** for building the application

---

## Deployment Options

### Option 1: Deploy to Render.com (FREE)

**Step 1: Prepare the Project**
```bash
# Create Dockerfile in project root
cd corporate-knowledge-base
```

Create `Dockerfile`:
```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Step 2: Push to GitHub**
```bash
git init
git add .
git commit -m "Initial commit"
git remote add origin YOUR_GITHUB_REPO_URL
git push -u origin main
```

**Step 3: Deploy on Render**
1. Go to https://render.com and sign up
2. Click "New +" → "Web Service"
3. Connect your GitHub repository
4. Configure:
   - **Name**: corporate-knowledge-base
   - **Environment**: Docker
   - **Instance Type**: Free
   - **Add Environment Variables**:
     ```
     SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR_NEON_HOST:5432/neondb?sslmode=require
     SPRING_DATASOURCE_USERNAME=neondb_owner
     SPRING_DATASOURCE_PASSWORD=npg_y6ga5MwYFVSb
     GEMINI_API_KEY=AIzaSyDIRej1JqgHEJRV5N5kgvutY580N9MPRvo
     ```
5. Click "Create Web Service"
6. Your app will be live at: `https://your-app-name.onrender.com`

---

### Option 2: Deploy to Railway.app (FREE Tier)

**Step 1: Install Railway CLI**
```bash
npm install -g @railway/cli
railway login
```

**Step 2: Initialize and Deploy**
```bash
cd corporate-knowledge-base
railway init
railway up
```

**Step 3: Add Environment Variables**
```bash
railway variables set SPRING_DATASOURCE_URL="jdbc:postgresql://YOUR_NEON_HOST:5432/neondb?sslmode=require"
railway variables set GEMINI_API_KEY="AIzaSyDIRej1JqgHEJRV5N5kgvutY580N9MPRvo"
```

**Step 4: Generate Domain**
```bash
railway domain
```

Your app is live!

---

### Option 3: Deploy to AWS Elastic Beanstalk

**Step 1: Package the Application**
```bash
mvn clean package
```

**Step 2: Create application.properties for production**
Update `src/main/resources/application-prod.properties`:
```properties
spring.datasource.url=${DATABASE_URL}
gemini.api.key=${GEMINI_API_KEY}
server.port=5000
```

**Step 3: Deploy**
1. Install AWS EB CLI: `pip install awsebcli`
2. Initialize: `eb init -p corretto-17 corporate-kb`
3. Create environment: `eb create prod-env`
4. Set environment variables:
   ```bash
   eb setenv DATABASE_URL="jdbc:postgresql://YOUR_NEON_HOST:5432/neondb?sslmode=require"
   eb setenv GEMINI_API_KEY="AIzaSyDIRej1JqgHEJRV5N5kgvutY580N9MPRvo"
   ```
5. Deploy: `eb deploy`

---

## Environment Variables for Deployment

**Required Variables:**
```
SPRING_DATASOURCE_URL=jdbc:postgresql://ep-sweet-shadow-ai0pynn4-pooler.c-4.us-east-1.aws.neon.tech:5432/neondb?sslmode=require
SPRING_DATASOURCE_USERNAME=neondb_owner
SPRING_DATASOURCE_PASSWORD=npg_y6ga5MwYFVSb
GEMINI_API_KEY=AIzaSyDIRej1JqgHEJRV5N5kgvutY580N9MPRvo
```

**Optional Variables:**
```
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod
```

---

## Database (Already Configured - Neon PostgreSQL)

Your Neon database is already set up and accessible:
- **Host**: ep-sweet-shadow-ai0pynn4-pooler.c-4.us-east-1.aws.neon.tech
- **Database**: neondb
- **Username**: neondb_owner
- **Password**: npg_y6ga5MwYFVSb
- **pgvector**: v0.8.0 (Already enabled)

No changes needed - works from anywhere!

---

## API Configuration (Already Set Up)

Your Gemini API key is configured and working:
- **API Key**: AIzaSyDIRej1JqgHEJRV5N5kgvutY580N9MPRvo
- **Model**: gemini-1.5-flash-latest
- **Endpoint**: https://generativelanguage.googleapis.com/v1

The application has automatic fallback if API fails.

---

## Testing Your Deployed Application

Once deployed, test these endpoints:

**1. Health Check**
```bash
curl https://your-app.com/api/documents
```

**2. Upload Document**
```bash
curl -X POST https://your-app.com/api/documents/upload \
  -F "file=@document.pdf"
```

**3. Query**
```bash
curl -X POST https://your-app.com/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is this about?"}'
```

---

## Build for Production (Optional)

To create optimized production JAR:
```bash
mvn clean package -Pprod
```

Output: `target/corporate-knowledge-base-1.0.0.jar`

Run locally:
```bash
java -jar target/corporate-knowledge-base-1.0.0.jar
```

---

## Security Recommendations for Production

1. **Use Environment Variables** (not hardcoded credentials)
2. **Enable HTTPS** (Most platforms provide free SSL)
3. **Add rate limiting** to API endpoints
4. **Set up CORS properly** for your production domain
5. **Monitor API usage** (Gemini API has quotas)

---

## Cost Estimate (FREE Options)

- **Neon Database**: FREE (Your current plan)
- **Render/Railway**: FREE tier available
- **Gemini API**: FREE tier (15 requests/minute)
- **Total**: $0/month for demo/interview use

For production:
- **Paid hosting**: $7-20/month
- **Gemini API**: Pay-as-you-go after free tier

---

## For Interview Demo

**Best Option: Render.com**
- Deployment time: 5-10 minutes
- Free forever
- Auto-deploys from GitHub
- Free SSL certificate
- Custom domain support

**Live URL Format:**
`https://corporate-knowledge-base.onrender.com`

---

## Need Help?

1. Application logs: Check your hosting platform dashboard
2. Database issues: Check Neon console at neon.tech
3. API issues: Check Google Cloud Console

Your configuration is production-ready!
