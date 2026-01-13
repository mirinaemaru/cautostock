# Restart Skill

Restart the local development server quickly.

## Steps

1. Stop existing server:
```bash
./stop-maruweb.sh
```

2. Start server:
```bash
./run-maruweb.sh
```

3. Wait and verify (15 seconds):
```bash
sleep 15
curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/
```

4. Show recent logs:
```bash
tail -20 /tmp/maruweb.log
```

## Expected Result

- HTTP 200: "Server restarted successfully on http://localhost:8090"
- Otherwise: Show last 30 lines of log for debugging
