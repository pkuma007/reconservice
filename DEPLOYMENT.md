# Deployment Guide - CSV Reconciliation Service

This guide will help you deploy the CSV Reconciliation Service to Replit (truly free, no payment method required).

## Prerequisites

- Replit account (free tier - no payment method required)
- GitHub account (optional, for backup)
- Git installed on your local machine (optional)

## Deployment Steps

### 1. Create Replit Account

1. Go to [Replit](https://replit.com)
2. Click "Sign up"
3. Sign up with email, Google, or GitHub
4. No payment method required for free tier
5. Verify your email address

### 2. Create a New Repl

1. Click "Create Repl"
2. Select "Java" as the template
3. Name your repl: `reconservice`
4. Click "Create Repl"

### 3. Upload Your Project

**Option A: Import from GitHub (Recommended)**

1. In your Repl, click the "+" button in the file tree
2. Select "Import from GitHub"
3. Enter your repository URL: `https://github.com/YOUR_USERNAME/reconservice.git`
4. Click "Import"

**Option B: Upload Files Manually**

1. In your Repl, click the "+" button in the file tree
2. Select "Upload file"
3. Upload all files from your project:
   - `pom.xml`
   - All files in `src/` directory
   - All files in `src/main/resources/` directory

### 4. Configure the Repl

1. Open the `.replit` file (create if it doesn't exist)
2. Add the following configuration:

```toml
[run]
command = "mvn spring-boot:run"
```

3. Set the port:
   - Click "Shell" tab
   - Run: `export PORT=8080`
   - Or add to `.replit`:
   ```toml
   [env]
   PORT = "8080"
   ```

### 5. Build and Run the Application

1. Click the "Run" button at the top
2. Replit will automatically:
   - Install Maven dependencies
   - Build the application
   - Start the Spring Boot application
3. Wait for the build to complete (first time may take 2-3 minutes)

### 6. Access Your Application

1. Once the application starts, Replit will show a "Webview" window
2. Or click the URL shown in the console (e.g., `https://reconservice-username.replit.co`)
3. Your CSV Reconciliation Service should now be live!

## Important Notes

### Free Tier Limitations

- Replit free tier includes:
  - 512 MB RAM
  - Shared CPU
  - Always running (no sleep limitation)
  - Public URL provided
  - Truly free forever with no payment method required
  - Best suited for development and small projects

### File Upload Size

- The free tier may have limitations on large file uploads
- For production use with large files, consider upgrading to a paid tier

### Environment Variables

If you need to add environment variables:
1. Open the `.replit` file
2. Add under [env] section:
   ```toml
   [env]
   PORT = "8080"
   YOUR_VAR = "value"
   ```
3. Or use the "Secrets" tab in Replit (for sensitive values)

### Monitoring Logs

- View logs in the "Console" tab at the bottom
- Monitor for any errors during deployment or runtime
- Check the "Shell" tab for system-level logs

## Troubleshooting

### Build Fails

- Check the build logs in the Console tab
- Ensure Maven dependencies are correct in pom.xml
- Verify Java version compatibility (requires Java 11+)
- Check internet connectivity in Replit

### Application Won't Start

- Check the logs in the Console tab
- Ensure the .replit file has correct run command
- Verify port configuration (default: 8080)
- Try stopping and clicking "Run" again

### Connection Issues

- Check if the Repl is running (green dot next to Run button)
- Verify the URL is correct
- Check if the webview is showing properly
- Try opening the URL in a new tab

## Updates and Redeployment

After making changes to your code:

**If using GitHub integration:**
1. Push changes to GitHub
2. In Replit, click "Git" tab
3. Click "Pull" to get latest changes
4. Click "Run" to restart the application

**If editing directly in Replit:**
1. Make changes in the file editor
2. Click "Run" to restart the application
3. Changes will be automatically applied

## Security Considerations

- Use Replit's "Secrets" tab for sensitive configuration
- Don't commit sensitive data (API keys, secrets) to git
- Make your Repl private if needed (still free)
- Regularly update dependencies for security patches
- Be cautious about sharing public Repl URLs with sensitive data
