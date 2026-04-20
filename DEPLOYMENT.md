# Deployment Guide - CSV Reconciliation Service

This guide will help you deploy the CSV Reconciliation Service to Render.com (free tier) using GitHub.

## Prerequisites

- GitHub account
- Render.com account (free tier)
- Git installed on your local machine
- Maven installed on your local machine

## Deployment Steps

### 1. Create GitHub Repository

1. Go to [GitHub](https://github.com) and sign in
2. Click the "+" button in the top-right corner
3. Select "New repository"
4. Repository name: `reconservice` (or your preferred name)
5. Make it **Public** (required for Render.com free tier)
6. Initialize with: **Add a README file** (optional)
7. Click "Create repository"

### 2. Initialize Git and Push to GitHub

Open your terminal in the project directory and run:

```bash
# Initialize git if not already done
git init

# Add all files to git
git add .

# Commit changes
git commit -m "Initial commit - CSV Reconciliation Service"

# Add remote repository (replace YOUR_USERNAME with your GitHub username)
git remote add origin https://github.com/YOUR_USERNAME/reconservice.git

# Push to GitHub
git branch -M main
git push -u origin main
```

### 3. Deploy to Render.com

#### Option A: Using render.yaml (Recommended)

1. Go to [Render.com](https://render.com) and sign in/sign up
2. Click "New" in the dashboard
3. Select "Web Service"
4. Click "Build and deploy from a Git repository"
5. Connect your GitHub account if not already connected
6. Select the `reconservice` repository
7. Render will automatically detect the `render.yaml` file
8. Click "Create Web Service"
9. Your app will be deployed automatically

#### Option B: Manual Configuration

1. Go to [Render.com](https://render.com) and sign in/sign up
2. Click "New" in the dashboard
3. Select "Web Service"
4. Click "Build and deploy from a Git repository"
5. Connect your GitHub account if not already connected
6. Select the `reconservice` repository
7. Configure:
   - **Name**: `reconservice`
   - **Region**: Oregon (or closest to you)
   - **Branch**: `main`
   - **Runtime**: Java
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/reconservice-1.0-SNAPSHOT.jar`
8. Click "Create Web Service"
9. Your app will be deployed automatically

### 4. Wait for Deployment

- Render will build and deploy your application
- This typically takes 2-5 minutes for the first deployment
- You can monitor the progress in the Render dashboard
- Once deployed, you'll get a URL like: `https://reconservice.onrender.com`

### 5. Access Your Application

- Click on the URL provided by Render
- Your CSV Reconciliation Service should now be live on the internet!

## Important Notes

### Free Tier Limitations

- Render free tier has:
  - 512 MB RAM
  - 0.1 CPU
  - Sleeps after 15 minutes of inactivity
  - Takes ~30 seconds to wake up from sleep
  - 750 hours/month usage limit

### File Upload Size

- The free tier may have limitations on large file uploads
- For production use with large files, consider upgrading to a paid tier

### Environment Variables

If you need to add environment variables:
1. Go to your Render dashboard
2. Select your service
3. Click "Environment" tab
4. Add any required environment variables

### Monitoring Logs

- View logs in Render dashboard under "Logs" tab
- Monitor for any errors during deployment or runtime

## Alternative Free Hosting Options

### Railway.app

1. Create account at [Railway.app](https://railway.app)
2. Click "New Project" → "Deploy from GitHub repo"
3. Select your repository
4. Railway will auto-detect the Java/Maven setup
5. Deploy automatically

### Fly.io

1. Install Fly CLI: `curl -L https://fly.io/install.sh | sh`
2. Sign up and login: `fly auth signup`
3. Initialize: `fly launch`
4. Deploy: `fly deploy`

## Troubleshooting

### Build Fails

- Check the build logs in Render dashboard
- Ensure Maven dependencies are correct
- Verify Java version compatibility (requires Java 11+)

### Application Won't Start

- Check the logs in Render dashboard
- Ensure the start command is correct
- Verify port configuration (default: 8080)

### 502 Bad Gateway

- Application might be starting up
- Wait 1-2 minutes and refresh
- Check if application is sleeping and needs to wake up

## Updates and Redeployment

After making changes to your code:

```bash
git add .
git commit -m "Your commit message"
git push
```

Render will automatically detect the push and redeploy your application.

## Security Considerations

- Keep your repository public for Render free tier
- Don't commit sensitive data (API keys, secrets)
- Use environment variables for sensitive configuration
- Regularly update dependencies for security patches
