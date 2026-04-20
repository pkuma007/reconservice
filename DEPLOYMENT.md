# Deployment Guide - CSV Reconciliation Service

This guide will help you deploy the CSV Reconciliation Service to Oracle Cloud Always Free (truly free, no payment method required).

## Prerequisites

- GitHub account
- Oracle Cloud account (Always Free tier - no payment method required)
- Git installed on your local machine
- Maven installed on your local machine
- SSH client installed

## Deployment Steps

### 1. Create GitHub Repository

1. Go to [GitHub](https://github.com) and sign in
2. Click the "+" button in the top-right corner
3. Select "New repository"
4. Repository name: `reconservice` (or your preferred name)
5. Make it **Public** (optional but recommended)
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

### 3. Deploy to Oracle Cloud Always Free

Oracle Cloud offers a truly free tier with no payment method required:

1. **Create Oracle Cloud Account:**
   - Go to https://www.oracle.com/cloud/free/
   - Click "Try Free"
   - Sign up with your email
   - No payment method required for Always Free tier
   - Verify your email address

2. **Create a Compute Instance:**
   - Go to Oracle Cloud Console
   - Navigate to "Compute" → "Instances"
   - Click "Create Instance"
   - Configure:
     - **Name**: `reconservice`
     - **Compartment**: Select your compartment
     - **Availability Domain**: Any
     - **Shape**: Always Free (VM.Standard.E2.1.Micro)
     - **Operating System**: Oracle Linux or Ubuntu
     - **SSH Keys**: Add your SSH public key
   - Click "Create"

3. **Connect to Your Instance:**
   ```bash
   ssh -i /path/to/your/private-key opc@<your-instance-public-ip>
   ```

4. **Install Java and Maven on the Instance:**
   ```bash
   sudo yum update -y
   sudo yum install -y java-11-openjdk-devel
   sudo yum install -y maven
   java -version
   mvn -version
   ```

5. **Clone Your Repository:**
   ```bash
   git clone https://github.com/YOUR_USERNAME/reconservice.git
   cd reconservice
   ```

6. **Build the Application:**
   ```bash
   mvn clean package -DskipTests
   ```

7. **Run the Application:**
   ```bash
   java -jar target/reconservice-1.0-SNAPSHOT.jar
   ```

8. **Configure Firewall:**
   - Go to Oracle Cloud Console
   - Navigate to "Networking" → "Virtual Cloud Networks"
   - Add an ingress rule to allow traffic on port 8080 from 0.0.0.0/0

9. **Set Up as a Service (Optional):**
   ```bash
   sudo vi /etc/systemd/system/reconservice.service
   ```
   Add:
   ```ini
   [Unit]
   Description=Reconciliation Service
   After=network.target

   [Service]
   Type=simple
   User=opc
   WorkingDirectory=/home/opc/reconservice
   ExecStart=/usr/bin/java -jar /home/opc/reconservice/target/reconservice-1.0-SNAPSHOT.jar
   Restart=on-failure

   [Install]
   WantedBy=multi-user.target
   ```
   Then:
   ```bash
   sudo systemctl enable reconservice
   sudo systemctl start reconservice
   ```

### 4. Wait for Deployment

- The application will start immediately after building
- This typically takes 5-10 minutes for the first deployment
- You can monitor the application with: `sudo systemctl status reconservice`
- Once deployed, access via: `http://<your-instance-public-ip>:8080`

### 5. Access Your Application

- Open your browser and navigate to: `http://<your-instance-public-ip>:8080`
- Your CSV Reconciliation Service should now be live on the internet!

## Important Notes

### Free Tier Limitations

- Oracle Cloud Always Free includes:
  - 2 AMD-based compute instances (VM.Standard.E2.1.Micro)
  - 4 ARM-based Ampere A1 cores and 24 GB memory
  - 200 GB block volume storage
  - 10 TB/month outbound data transfer
  - Truly free forever with no payment method required

### File Upload Size

- The free tier may have limitations on large file uploads
- For production use with large files, consider upgrading to a paid tier

### Environment Variables

If you need to add environment variables:
1. SSH into your instance
2. Edit the service file: `sudo vi /etc/systemd/system/reconservice.service`
3. Add under [Service] section:
   ```ini
   Environment="PORT=8080"
   Environment="YOUR_VAR=value"
   ```
4. Reload and restart:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl restart reconservice
   ```

### Monitoring Logs

- View logs with: `sudo journalctl -u reconservice -f`
- Monitor for any errors during deployment or runtime
- Check application status: `sudo systemctl status reconservice`

## Troubleshooting

### Build Fails

- Check the build logs in the terminal
- Ensure Maven dependencies are correct
- Verify Java version compatibility (requires Java 11+)
- Check internet connectivity on the instance

### Application Won't Start

- Check the logs: `sudo journalctl -u reconservice -f`
- Ensure the JAR file exists: `ls -la target/reconservice-1.0-SNAPSHOT.jar`
- Verify port configuration (default: 8080)
- Check service status: `sudo systemctl status reconservice`

### Connection Issues

- Check if the instance is running in Oracle Cloud Console
- Verify firewall rules allow port 8080
- Check security list ingress rules
- Ensure the instance has a public IP address

## Updates and Redeployment

After making changes to your code:

```bash
git add .
git commit -m "Your commit message"
git push
```

Then redeploy on the Oracle Cloud instance:

```bash
ssh -i /path/to/your/private-key opc@<your-instance-public-ip>
cd reconservice
git pull
mvn clean package -DskipTests
sudo systemctl restart reconservice
```

## Security Considerations

- Use SSH keys for instance access (not passwords)
- Configure firewall rules to restrict access
- Don't commit sensitive data (API keys, secrets) to git
- Use environment variables for sensitive configuration
- Regularly update the OS and dependencies for security patches
- Keep your SSH private key secure
