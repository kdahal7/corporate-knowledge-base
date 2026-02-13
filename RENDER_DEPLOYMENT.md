# 🚀 Deploy to Render.com (FREE - No Credit Card!)

## ✅ Prerequisites Complete
- Code is on GitHub: https://github.com/kdahal7/corporate-knowledge-base
- Hugging Face API Key: Get yours at https://huggingface.co/settings/tokens
- Application is ready to deploy

---

## 📝 5-Minute Deployment Steps

### Step 1: Sign Up on Render
1. Go to: **https://render.com/**
2. Click **"Get Started for Free"**
3. Sign up with GitHub (recommended) or email
4. No credit card required! ✅

### Step 2: Create New Web Service
1. Click **"New +"** (top right)
2. Select **"Web Service"**
3. Connect your GitHub account if not already
4. Select repository: **`kdahal7/corporate-knowledge-base`**
5. Click **"Connect"**

### Step 3: Configure Your Service
Fill in these settings:

**Basic Settings:**
- **Name**: `kb-app` (or any name you like)
- **Region**: Choose closest to you (e.g., Oregon, Frankfurt)
- **Branch**: `main`
- **Root Directory**: (leave empty)

**Build & Deploy Settings:**
- **Runtime**: `Docker`
- **Dockerfile Path**: `Dockerfile` (auto-detected)
- Build and Start commands are handled by Dockerfile automatically ✅

**Plan:**
- Select **"Free"** (750 hours/month)

### Step 4: Add Environment Variables
Scroll down to **"Environment Variables"** section and add:

| Key | Value |
|-----|-------|
| `HUGGINGFACE_API_KEY` | `YOUR_HF_API_KEY_HERE` (Get from https://huggingface.co/settings/tokens) |
| `LLM_PROVIDER` | `huggingface` |

Click **"Add Environment Variable"** for each one.

### Step 5: Deploy!
1. Click **"Create Web Service"** at the bottom
2. Render will start building your app
3. Wait 5-10 minutes for first build
4. Your app will be live at: `https://kb-app.onrender.com`

---

## 🎉 That's It!

Your app is now deployed and accessible online!

### 📊 Monitoring Your App

**View Logs:**
- Go to your service dashboard
- Click **"Logs"** tab
- See real-time application logs

**Check Status:**
- Dashboard shows if service is running
- Green = Active and healthy

---

## ⚠️ Important Notes

### Free Tier Limitations:
- **Spins down after 15 minutes** of inactivity
- First request after spin-down takes ~30-60 seconds
- 750 free hours per month (enough for hobby projects)

### Database:
- Your Neon.tech PostgreSQL is already configured
- No changes needed - it will work on Render

### Updates:
When you push to GitHub:
1. Render auto-detects changes
2. Automatically rebuilds and redeploys
3. Zero downtime deployment!

---

## 🔧 Troubleshooting

### Build Fails?
- Check Build Logs in Render dashboard
- Most common: Docker build issues or dependency problems
- Verify Dockerfile is in root directory of repo

### App Crashes?
- Check Runtime Logs
- Verify environment variables are set correctly
- Check database connection string

### Slow First Load?
- Normal! Free tier spins down after inactivity
- Consider upgrading to paid plan for always-on service

---

## 💡 Tips

1. **Custom Domain**: Can add your own domain in Render settings (free)
2. **Auto-Deploy**: Already enabled - push to GitHub = auto deploy
3. **Scaling**: Easy to upgrade to paid plan if needed
4. **Health Checks**: Render automatically monitors your app

---

## 🆘 Need Help?

- **Render Docs**: https://render.com/docs
- **Community Forum**: https://community.render.com/
- **Support**: Available via dashboard

---

**Ready? Go to https://render.com/ and follow the steps above!** 🚀
