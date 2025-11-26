# Quick Start Guide

## 🚀 Start Everything

```bash
./start.sh
```

This script will:
1. ✅ Check Docker is running
2. ✅ Start PostgreSQL and RabbitMQ containers
3. ✅ Build all Maven modules
4. ✅ Start all services in background:
   - Orchestrator Service (port 8081)
   - Researcher Agent
   - Writer Agent
   - Evaluator Agent
5. ✅ Verify everything is running

**Logs are saved to:** `logs/` directory

---

## 🛑 Stop Everything

```bash
./shutdown.sh
```

This script will:
1. ✅ Stop all Spring Boot services
2. ✅ Stop Docker containers
3. ✅ Clean up PID files
4. ✅ Verify everything is stopped

---

## 🧪 Test the Workflow

```bash
./test-workflow.sh
```

This script will:
1. ✅ Check services are running
2. ✅ Create a test content request
3. ✅ Monitor workflow progress (RESEARCHER → WRITER → EVALUATOR)
4. ✅ Show final status and content

---

## 📋 Manual Testing

### 1. Create a Request

```bash
curl -X POST http://localhost:8081/api/v1/requests \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "AI in Healthcare",
    "tone": "professional",
    "language": "English"
  }'
```

**Response:**
```json
{
  "request_id": 1,
  "status": "PENDING",
  "topic": "AI in Healthcare",
  "message": "Content request created successfully"
}
```

### 2. Check Status

```bash
curl http://localhost:8081/api/v1/requests/1/status
```

**Response:**
```json
{
  "request_id": 1,
  "status": "IN_PROGRESS",
  "topic": "AI in Healthcare",
  "tasks": [
    {
      "id": 1,
      "agent_type": "RESEARCHER",
      "status": "COMPLETED"
    },
    {
      "id": 2,
      "agent_type": "WRITER",
      "status": "COMPLETED"
    },
    {
      "id": 3,
      "agent_type": "EVALUATOR",
      "status": "IN_PROGRESS"
    }
  ]
}
```

### 3. Get Generated Content

```bash
curl http://localhost:8081/api/v1/requests/1/content
```

---

## 🔍 Check Logs

```bash
# View orchestrator logs
tail -f logs/orchestrator-service.log

# View researcher agent logs
tail -f logs/researcher-agent.log

# View writer agent logs
tail -f logs/writer-agent.log

# View evaluator agent logs
tail -f logs/evaluator-agent.log
```

---

## 🌐 Access Services

- **Orchestrator API:** http://localhost:8081
- **RabbitMQ Management UI:** http://localhost:15672
  - Username: `admin`
  - Password: `admin123`
- **PostgreSQL:** localhost:5432
  - Database: `growthengine`
  - Username: `admin`
  - Password: `admin123`

---

## 🐛 Troubleshooting

### Services won't start?

1. Check Docker is running:
   ```bash
   docker ps
   ```

2. Check if ports are in use:
   ```bash
   lsof -i :8081
   lsof -i :5432
   lsof -i :5672
   ```

3. Check logs:
   ```bash
   ls -la logs/
   tail -f logs/orchestrator-service.log
   ```

### Build fails?

```bash
# Clean and rebuild
mvn clean install -DskipTests
```

### Services not processing tasks?

1. Check RabbitMQ queues: http://localhost:15672 → Queues
2. Check all services are running:
   ```bash
   ps aux | grep java | grep -E "(orchestrator|researcher|writer|evaluator)"
   ```
3. Check database:
   ```bash
   docker exec growth-engine-postgres psql -U admin -d growthengine -c "SELECT * FROM tasks ORDER BY id DESC LIMIT 5;"
   ```

---

## 📊 Current Workflow

```
User Request
    ↓
RESEARCHER Agent (Research topic)
    ↓
WRITER Agent (Write content)
    ↓
EVALUATOR Agent (Evaluate quality) ✅
    ↓
(Next: SEO Agent)
    ↓
(Next: Publisher Agent)
```

---

## 🎯 Next Steps

After testing Steps 18 & 19:
- ✅ Step 18: Evaluator Agent - Complete
- ✅ Step 19: Writer → Evaluator Flow - Complete
- 🎯 Step 20: Create SEO Agent
- 🎯 Step 21: Evaluator → SEO Flow

---

## 💡 Tips

1. **Always use `./shutdown.sh` before stopping** to clean up properly
2. **Check logs** if something doesn't work
3. **Wait 10-15 seconds** after starting services before testing
4. **Use `./test-workflow.sh`** for automated testing

---

Happy coding! 🚀

