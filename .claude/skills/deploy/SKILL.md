---
name: deploy
description: Deploy the application through Jenkins pipeline. Use when user asks to deploy, push to production, check deployment status, or verify app health.
allowed-tools: Bash, Read, Grep, Glob
---

# Deploy Skill

This skill handles deployment operations for the Maru Web application.

## Deployment Process

### 1. Pre-deployment Checks
- Verify current git status
- Check if there are uncommitted changes
- Ensure we're on the master branch

### 2. Build & Test
```bash
# Clean and build
./mvnw clean package -DskipTests

# Run tests
./mvnw test
```

### 3. Push to GitHub (triggers Jenkins)
```bash
# Push changes to trigger Jenkins pipeline
git push origin master
```

### 4. Monitor Jenkins Build
- Jenkins URL: http://localhost:9090/job/maruweb-local/
- Jenkins automatically:
  - Checks out code
  - Builds with Maven
  - Runs tests
  - Stops previous instance on port 9080
  - Deploys new JAR to /opt/maruweb/
  - Performs health check

### 5. Verify Deployment
```bash
# Check if app is running
curl -s -o /dev/null -w "%{http_code}" http://localhost:9080/

# Check application logs
tail -f /opt/maruweb/application.log

# Check process
lsof -ti:9080
```

## Deployment Configuration

**Build Tool**: Maven 3.9
**Java Version**: JDK 17
**Deploy Port**: 9080
**Deploy Directory**: /opt/maruweb/
**Artifact**: todo-0.0.1-SNAPSHOT.jar

## Environment Variables (Jenkins)
- `GOOGLE_CLIENT_ID`: Google OAuth credentials
- `GOOGLE_CLIENT_SECRET`: Google OAuth credentials
- `CALENDAR_ENCRYPTION_KEY`: Calendar data encryption key

## Common Commands

### Quick Deploy
```bash
# Build, commit, push (triggers Jenkins)
./mvnw clean package -DskipTests && git add . && git commit -m "Deploy" && git push
```

### Manual Deploy (bypass Jenkins)
```bash
# Build
./mvnw clean package -DskipTests

# Stop current instance
kill -9 $(lsof -ti:9080)

# Start new instance
nohup java -jar target/todo-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --server.port=9080 > /opt/maruweb/application.log 2>&1 &
```

### Health Check
```bash
# Check HTTP status
curl -I http://localhost:9080/

# Check logs for errors
grep -i error /opt/maruweb/application.log | tail -20
```

## Troubleshooting

### Build Fails
- Check `mvn -v` for correct Java version (17)
- Review error logs in Jenkins console output
- Verify all tests pass: `./mvnw test`

### Deployment Fails
- Check port 9080 is not blocked: `lsof -ti:9080`
- Verify Jenkins credentials are configured
- Check disk space: `df -h`
- Review application logs: `tail -f /opt/maruweb/application.log`

### App Not Responding
- Check process is running: `ps aux | grep maruweb`
- Check logs: `tail -100 /opt/maruweb/application.log`
- Verify database connectivity
- Check environment variables are set

## Instructions for Claude

When user asks to deploy:

1. **Check git status** - ensure all changes are committed
2. **Ask user confirmation** - "Ready to deploy to production?"
3. **Run tests** - `./mvnw test` and verify they pass
4. **Push to GitHub** - This triggers Jenkins pipeline
5. **Wait 2-3 minutes** - Jenkins needs time to build and deploy
6. **Verify deployment** - Check http://localhost:9080/ returns 200
7. **Report results** - Tell user deployment status and provide Jenkins URL

When user asks to check deployment:

1. **Check app health** - `curl http://localhost:9080/`
2. **Check Jenkins** - Provide Jenkins build URL
3. **Check logs** - `tail /opt/maruweb/application.log`
4. **Report status** - Running/Not Running, last build info

## Examples

**User**: "Deploy the app"
**Claude**: Runs tests, pushes to GitHub, waits for Jenkins, verifies deployment

**User**: "Is the app deployed?"
**Claude**: Checks port 9080, Jenkins status, reports current state

**User**: "Check deployment logs"
**Claude**: Shows recent application logs from /opt/maruweb/application.log
