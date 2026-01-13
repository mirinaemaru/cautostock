# Logs Skill

Show and analyze server logs for the maruweb application.

## Default Action

Show last 50 lines of logs:
```bash
tail -50 /tmp/maruweb.log
```

## Options

### Recent Errors Only
```bash
grep -i "ERROR\|WARN\|Exception" /tmp/maruweb.log | tail -30
```

### Follow Mode (real-time)
```bash
tail -f /tmp/maruweb.log
```

### Search for Specific Pattern
```bash
grep -i "<pattern>" /tmp/maruweb.log | tail -30
```

## Log Analysis

When showing logs, highlight:
- **ERROR**: Critical issues requiring immediate attention
- **WARN**: Potential problems
- **Exception/Stacktrace**: Code errors with line numbers

## Common Log Patterns

| Pattern | Meaning |
|---------|---------|
| `Started TodoApplication` | Server started successfully |
| `Tomcat started on port` | Server ready to accept requests |
| `Connection refused` | Database or external service unavailable |
| `OAuth` errors | Google Calendar authentication issues |
| `TradingApiService` | Trading System API connection issues |

## Instructions

1. Default: Show last 50 lines
2. If user mentions "error" or "에러": Filter for errors only
3. If user mentions "follow" or "실시간": Use tail -f (warn it will run continuously)
4. Always summarize key findings after showing logs
