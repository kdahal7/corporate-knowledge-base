# Hugging Face Setup Guide (FREE Cloud LLM)

## Why Hugging Face?

- ✅ **100% FREE** tier with generous limits
- ✅ **No credit card** required
- ✅ **Production-ready** for deployment
- ✅ **Fast inference** (2-3 seconds)
- ✅ **Works on Render, Railway, AWS** etc.

## Step 1: Get FREE API Key

1. Go to https://huggingface.co/join
2. Create a free account (no payment needed)
3. Navigate to: https://huggingface.co/settings/tokens
4. Click **"New token"**
5. Name: `corporate-kb-api`
6. Role: **Read**
7. Click **"Generate a token"**
8. Copy your token (starts with `hf_...`)

## Step 2: Configure Application

### For Local Development:

Edit `src/main/resources/application.properties`:

```properties
# Use Hugging Face for cloud compatibility
llm.provider=huggingface

# Add your token
huggingface.api.key=hf_YOUR_TOKEN_HERE
```

### For Cloud Deployment (Render/Railway/AWS):

Set environment variable:
```bash
HUGGINGFACE_API_KEY=hf_YOUR_TOKEN_HERE
```

## Step 3: Test Locally

```bash
# Restart application
mvn spring-boot:run

# Test query (should take 2-4 seconds)
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question":"What is my CGPA?","topK":2}'
```

## Switching Between Providers

### Use Ollama (Local - Development):
```properties
llm.provider=ollama
```

### Use Hugging Face (Cloud - Production):
```properties
llm.provider=huggingface
```

## Free Tier Limits

- **Rate limit**: 1000 requests/hour
- **Token limit**: Unlimited
- **Cost**: $0 forever
- **Perfect for**: Demos, interviews, personal projects

## Alternative FREE LLM Options

If you need more requests:

1. **Together AI**: https://together.ai (free $25 credit)
2. **Replicate**: https://replicate.com (free tier)
3. **Groq**: https://console.groq.com (free, very fast)

## Deployment Tips

1. **Render.com**: Set `HUGGINGFACE_API_KEY` in environment variables
2. **Railway.app**: Add to project variables
3. **AWS Elastic Beanstalk**: Add to configuration
4. **Docker**: Pass as `ENV` variable

## Performance

- **Ollama** (local): 2-4 seconds ⚡
- **Hugging Face** (cloud): 2-5 seconds ⚡⚡
- **Gemini API** (cloud): Would be 1-2 seconds if quota available

## Interview Demo Strategy

**For your Infosys interview:**

1. Deploy to **Render.com** with Hugging Face (FREE)
2. Live URL: `https://your-app.onrender.com`
3. Show them:
   - Upload resume PDF
   - Ask intelligent questions
   - Get accurate AI responses
   - Explain RAG pipeline
   - Show vector similarity in action

**They'll be impressed that you:**
- Built production-ready RAG system
- Used pgvector for embeddings
- Integrated free cloud LLM
- Deployed live application
- All from scratch!

Good luck! 🚀
