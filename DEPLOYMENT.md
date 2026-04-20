# Deployment Guide - CSV Reconciliation Service

This guide will help you deploy the CSV Reconciliation Service to Fly.io (truly free tier) using GitHub.

## Prerequisites

- GitHub account
- Fly.io account (free tier)
- Git installed on your local machine
- Maven installed on your local machine

## Deployment Steps

### 1. Create GitHub Repository

1. Go to [GitHub](https://github.com) and sign in
2. Click the "+" button in the top-right corner
3. Select "New repository"
4. Repository name: `reconservice` (or your preferred name)
5. Make it **Public** (required for Fly.io free tier)
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

### 3. Deploy to Fly.io

Fly.io has a truly free tier with Java support:

1. Install Fly CLI (Mac/Linux):
   ```bash
   curl -L https://fly.io/install.sh | sh
   ```
   Or for Windows, download from https://fly.io/docs/hands-on/install-flyctl/

2. Sign up for Fly.io:
   ```bash
   fly auth signup
   ```

3. Login to Fly.io:
   ```bash
   fly auth login
   ```

4. Initialize Fly.io in your project:
   ```bash
   cd /Users/kupravin/IdeaProjects/reconservice
   fly launch
   ```

5. When prompted, configure:
   - App name: `reconservice` (or your preferred name)
   - Region: Select closest to you (e.g., sjc for San Francisco)
   - Deploy now: No (we need to configure first)

6. Create a fly.toml file (if not auto-created):
   ```bash
   fly launch --no-deploy
   ```

7. Edit the fly.toml file to configure Java:
   ```toml
   app = "reconservice"
   primary_region = "sjc"

   [build]
   builder = "heroku/buildpacks:20"

   [env]
   PORT = "8080"

   [services]
   [[services.ports]]
   handlers = ["http"]
   port = 8080

   [[services.ports]]
   handlers = ["tls", "http"]
   port = 443

   [[services.http_checks]]
   interval = 10000
   grace_period = "5s"
   method = "GET"
   path = "/"
   protocol = "http"
   timeout = 5000
   ```

8. Deploy your application:
   ```bash
   fly deploy
   ```

9. Your app will be deployed automatically (2-5 minutes)

### 4. Wait for Deployment

- Fly.io will build and deploy your application
- This typically takes 2-5 minutes for the first deployment
- You can monitor the progress with: `fly logs`
- Once deployed, you'll get a URL like: `https://reconservice.fly.dev`

### 5. Access Your Application

- Click on the URL provided by Fly.io or run: `fly open`
- Your CSV Reconciliation Service should now be live on the internet!

## Important Notes

### Free Tier Limitations

- Fly.io free tier includes:
  - Up to 3 apps
  - 256 MB RAM per app
  - Shared CPU
  - 3 GB volume storage
  - No sleep time limitation (always running)
  - Truly free forever

### File Upload Size

- The free tier may have limitations on large file uploads
- For production use with large files, consider upgrading to a paid tier

### Environment Variables

If you need to add environment variables:
1. Edit the fly.toml file
2. Add under [env] section:
   ```toml
   [env]
   PORT = "8080"
   YOUR_VAR = "value"
   ```
3. Or use: `fly secrets set YOUR_VAR=value`

### Monitoring Logs

- View logs with: `fly logs`
- Monitor for any errors during deployment or runtime
- View real-time logs with: `fly logs --tail`

## Alternative Free Hosting Options

### Oracle Cloud Always Free

If you need more resources, Oracle Cloud offers a truly free tier:
- 2 AMD-based compute instances (always free)
- 4 ARM-based Ampere A1 cores and 24 GB memory (always free)
- More complex setup but completely free
- Visit: https://www.oracle.com/cloud/free/

## Troubleshooting

### Build Fails

- Check the build logs with: `fly logs`
- Ensure Maven dependencies are correct
- Verify Java version compatibility (requires Java 11+)
- Check fly.toml configuration

### Application Won't Start

- Check the logs with: `fly logs`
- Ensure the start command is correct in fly.toml
- Verify port configuration (default: 8080)
- Check if the app is healthy: `fly status`

### Connection Issues

- Check if the app is running: `fly status`
- Verify the region is accessible
- Check firewall settings

## Updates and Redeployment

After making changes to your code:

```bash
git add .
git commit -m "Your commit message"
git push
```

Then redeploy to Fly.io:

```bash
fly deploy
```

## Security Considerations

- Keep your repository public for Fly.io free tier
- Don't commit sensitive data (API keys, secrets)
- Use fly secrets for sensitive configuration: `fly secrets set KEY=value`
- Regularly update dependencies for security patches
