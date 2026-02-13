# Quick Start Guide - 5 Minutes to Running

## Prerequisites Check

```bash
# Check Java (need 17+)
java -version

# Check Maven
mvn -version

# Check PostgreSQL (if using local)
psql --version
```

## Step 1: Get the Code (1 minute)

Already done! You're in the project directory.

## Step 2: Setup Database (2 minutes)

### Option A: Neon.tech (Fastest - No local install)

1. Open https://neon.tech/ and sign up (free)
2. Click "Create Project"
3. Copy your connection string from dashboard
4. Skip to Step 3

### Option B: Local PostgreSQL

```bash
# Create database
psql -U postgres -c "CREATE DATABASE corporate_kb;"

# Enable pgvector
psql -U postgres -d corporate_kb -c "CREATE EXTENSION vector;"
```

## Step 3: Get Gemini API Key (1 minute)

1. Visit https://makersuite.google.com/app/apikey
2. Click "Create API Key"
3. Copy the key (starts with `AIza...`)

## Step 4: Configure (1 minute)

Edit `src/main/resources/application.properties`:

```properties
# Database (choose one)
# -- For Neon --
spring.datasource.url=jdbc:postgresql://YOUR-PROJECT.neon.tech:5432/YOUR-DB?sslmode=require
spring.datasource.username=YOUR-USERNAME
spring.datasource.password=YOUR-PASSWORD

# -- For Local --
# spring.datasource.url=jdbc:postgresql://localhost:5432/corporate_kb
# spring.datasource.username=postgres
# spring.datasource.password=your_password

# Gemini API
gemini.api.key=YOUR-GEMINI-API-KEY
```

## Step 5: Run! (30 seconds)

```bash
# Build and run
mvn spring-boot:run
```

Wait for:
```
Started KnowledgeBaseApplication in 8.5 seconds
```

## Step 6: Use It!

1. **Open browser**: http://localhost:8080
2. **Upload a PDF**: Click upload area, select a PDF
3. **Wait**: Processing takes ~30 seconds for 50-page PDF
4. **Ask a question**: "What is the main topic of this document?"
5. **Get AI answer**: Powered by Gemini with source citations!

## Quick Test Commands

### Upload via curl
```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@sample.pdf"
```

### Query via curl
```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is this document about?", "topK": 3}'
```

### List documents
```bash
curl http://localhost:8080/api/documents
```

## Sample Questions to Try

After uploading an HR Policy PDF:
- "What is the sick leave policy?"
- "How many vacation days do employees get?"
- "What is the remote work policy?"
- "How do I request time off?"

## Troubleshooting

### "Failed to connect to database"
- **Check**: Database is running and connection string is correct
- **Fix**: Run `psql -U postgres -l` to verify database exists

### "Invalid API key"
- **Check**: API key is correct in `application.properties`
- **Fix**: Generate new key at https://makersuite.google.com/app/apikey

### "Port 8080 is already in use"
- **Fix**: Kill process using port:
  ```bash
  # Windows
  netstat -ano | findstr :8080
  taskkill /PID <PID> /F
  
  # Mac/Linux
  lsof -ti:8080 | xargs kill -9
  ```

### "Upload hangs"
- **Check**: PDF is not too large (< 50MB)
- **Check**: Internet connection for Gemini API calls
- **Fix**: Check application logs for errors

## What's Happening Behind the Scenes?

1. **Upload**: PDF → Text extraction (PDFBox)
2. **Chunking**: Split into 500-char overlapping chunks
3. **Embedding**: Each chunk → Gemini API → 768-dim vector
4. **Storage**: Vectors saved in PostgreSQL with pgvector
5. **Query**: Question → Embedding → Vector search → Top 3 chunks
6. **Generation**: Context + Question → Gemini → Answer

## Next Steps

- Read [README.md](README.md) for full documentation
- See [DATABASE_SETUP.md](DATABASE_SETUP.md) for advanced DB config
- Check [API_EXAMPLES.md](API_EXAMPLES.md) for more API usage

## Sample PDF for Testing

Don't have a PDF? Create one:

```bash
# Create a simple test document
echo "Employee Handbook

Leave Policy:
- Employees receive 15 vacation days per year
- Sick leave: 12 days annually
- Parental leave: 16 weeks paid

Remote Work:
- Employees can work remotely up to 3 days per week
- Must be available during core hours (10am-4pm)

Benefits:
- Health insurance provided
- 401k matching up to 6%
- Gym membership reimbursement" > test.txt

# Convert to PDF (you can use any online tool or this command if wkhtmltopdf is installed)
# Or just copy-paste into Word/Google Docs and save as PDF
```

---

**You're all set! 🎉**

Open http://localhost:8080 and start asking questions!
