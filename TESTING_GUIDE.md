# Testing Guide - Steps 17 & 25

## Prerequisites
- Docker installed and running
- Maven installed
- Java 21 installed

---

## STEP 1: Start Infrastructure Services (PostgreSQL & RabbitMQ)

### Open Terminal 1 - Start Docker Services

```bash
# Navigate to project root
cd /Users/devps/Downloads/growth-engine

# Start PostgreSQL and RabbitMQ
docker-compose up -d

# Verify services are running
docker ps
```

**Expected Output:**
```
CONTAINER ID   IMAGE                    STATUS         PORTS
xxx            pgvector/pgvector:pg16   Up 2 minutes   0.0.0.0:5432->5432/tcp
xxx            rabbitmq:3-management    Up 2 minutes   0.0.0.0:5672->5672/tcp, 0.0.0.0:15672->15672/tcp
```

**Wait 10-15 seconds** for services to fully start.

**Verify PostgreSQL:**
```bash
docker exec growth-engine-postgres psql -U admin -d growthengine -c "SELECT version();"
```

**Verify RabbitMQ:**
- Open browser: http://localhost:15672
- Login: admin / admin123
- You should see the RabbitMQ Management UI

---

## STEP 2: Build All Modules

### Open Terminal 2 - Build Project

```bash
# Navigate to project root
cd /Users/devps/Downloads/growth-engine

# Build all modules (this compiles all Java code)
mvn clean install -DskipTests

# Expected: BUILD SUCCESS
```

**If you see errors:**
- Make sure you've written all the code from Steps 17 & 25
- Check for compilation errors in the console

---

## STEP 3: Start Orchestrator Service

### Open Terminal 3 - Orchestrator Service

```bash
# Navigate to orchestrator-service directory
cd /Users/devps/Downloads/growth-engine/orchestrator-service

# Start the orchestrator service
mvn spring-boot:run
```

**Expected Output:**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.5.6)

... (Spring Boot startup logs)

Started OrchestratorApplication in X.XXX seconds
```

**Look for:**
- ✅ "Started OrchestratorApplication"
- ✅ "HikariPool-1 - Starting..." (database connection)
- ✅ No errors about RabbitMQ connection

**Keep this terminal open!** The service must keep running.

---

## STEP 4: Start Researcher Agent

### Open Terminal 4 - Researcher Agent

```bash
# Navigate to researcher-agent directory
cd /Users/devps/Downloads/growth-engine/researcher-agent

# Start the researcher agent
mvn spring-boot:run
```

**Expected Output:**
```
Started ResearcherAgentApplication in X.XXX seconds
```

**Look for:**
- ✅ "Started ResearcherAgentApplication"
- ✅ No errors

**Keep this terminal open!**

---

## STEP 5: Start Writer Agent

### Open Terminal 5 - Writer Agent

```bash
# Navigate to writer-agent directory
cd /Users/devps/Downloads/growth-engine/writer-agent

# Start the writer agent
mvn spring-boot:run
```

**Expected Output:**
```
Started WriterAgentApplication in X.XXX seconds
```

**Look for:**
- ✅ "Started WriterAgentApplication"
- ✅ No errors

**Keep this terminal open!**

---

## STEP 6: Verify All Services Are Running

**You should now have:**
- ✅ Terminal 1: Docker services (PostgreSQL + RabbitMQ)
- ✅ Terminal 3: Orchestrator Service (port 8081)
- ✅ Terminal 4: Researcher Agent
- ✅ Terminal 5: Writer Agent

**Quick Health Check:**
```bash
# Test orchestrator is responding
curl http://localhost:8081/api/v1/requests/999/status
```

**Expected:** `{"error":"Request not found"}` (this is good - means service is running!)

---

## STEP 7: Test Step 17 - Enhanced Status Endpoint

### Test 7.1: Create a New Request

**Open Terminal 6 (or use existing terminal):**

```bash
# Create a new content request
curl -X POST http://localhost:8081/api/v1/requests \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Artificial Intelligence in Healthcare",
    "tone": "professional",
    "language": "English",
    "user_id": 1
  }'
```

**Expected Response:**
```json
{
  "request_id": 1,
  "status": "PENDING",
  "topic": "Artificial Intelligence in Healthcare",
  "message": "Content request created successfully"
}
```

**Note the `request_id`** (let's say it's `1` for this example)

---

### Test 7.2: Check Status Immediately (Should Show PENDING)

```bash
# Replace 1 with your actual request_id
curl http://localhost:8081/api/v1/requests/1/status
```

**Expected Response (Step 17 - Enhanced):**
```json
{
  "request_id": 1,
  "status": "PENDING",
  "topic": "Artificial Intelligence in Healthcare",
  "tone": "professional",
  "language": "English",
  "created_at": "2024-01-15T10:30:00",
  "tasks": [
    {
      "id": 1,
      "agent_type": "RESEARCHER",
      "status": "PENDING",
      "created_at": "2024-01-15T10:30:00",
      "updated_at": "2024-01-15T10:30:00"
    }
  ]
}
```

**What to Verify:**
- ✅ `"status": "PENDING"` (Step 25 - initial status)
- ✅ `"tasks"` array is present (Step 17 - enhanced endpoint)
- ✅ Task shows `"agent_type": "RESEARCHER"`
- ✅ Task shows `"status": "PENDING"`

---

### Test 7.3: Wait 5-10 Seconds and Check Status Again

```bash
# Wait a few seconds, then check again
curl http://localhost:8081/api/v1/requests/1/status
```

**Expected Response (After RESEARCHER completes):**
```json
{
  "request_id": 1,
  "status": "IN_PROGRESS",
  "topic": "Artificial Intelligence in Healthcare",
  "tone": "professional",
  "language": "English",
  "created_at": "2024-01-15T10:30:00",
  "tasks": [
    {
      "id": 1,
      "agent_type": "RESEARCHER",
      "status": "COMPLETED",
      "created_at": "2024-01-15T10:30:00",
      "updated_at": "2024-01-15T10:30:05"
    },
    {
      "id": 2,
      "agent_type": "WRITER",
      "status": "IN_PROGRESS",
      "created_at": "2024-01-15T10:30:05",
      "updated_at": "2024-01-15T10:30:10"
    }
  ]
}
```

**What to Verify:**
- ✅ `"status": "IN_PROGRESS"` (Step 25 - status updated)
- ✅ RESEARCHER task shows `"status": "COMPLETED"`
- ✅ WRITER task appears in tasks array
- ✅ WRITER task shows `"status": "IN_PROGRESS"` or `"COMPLETED"`

---

### Test 7.4: Wait Another 5-10 Seconds (After WRITER Completes)

```bash
curl http://localhost:8081/api/v1/requests/1/status
```

**Expected Response (After WRITER completes):**
```json
{
  "request_id": 1,
  "status": "IN_PROGRESS",
  "topic": "Artificial Intelligence in Healthcare",
  "tone": "professional",
  "language": "English",
  "created_at": "2024-01-15T10:30:00",
  "tasks": [
    {
      "id": 1,
      "agent_type": "RESEARCHER",
      "status": "COMPLETED",
      "created_at": "2024-01-15T10:30:00",
      "updated_at": "2024-01-15T10:30:05"
    },
    {
      "id": 2,
      "agent_type": "WRITER",
      "status": "COMPLETED",
      "created_at": "2024-01-15T10:30:05",
      "updated_at": "2024-01-15T10:30:15"
    }
  ]
}
```

**What to Verify:**
- ✅ Both tasks show `"status": "COMPLETED"`
- ✅ Request status is still `"IN_PROGRESS"` (will change to COMPLETED when Publisher agent is added later)

---

## STEP 8: Test Step 25 - Status Updates Throughout Workflow

### Test 8.1: Create Another Request and Monitor Status Changes

```bash
# Create second request
curl -X POST http://localhost:8081/api/v1/requests \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Machine Learning Basics",
    "tone": "casual",
    "language": "English"
  }'
```

**Note the new `request_id`** (let's say it's `2`)

---

### Test 8.2: Monitor Status Progression

**Run this command multiple times (every 5 seconds):**

```bash
# Replace 2 with your actual request_id
curl http://localhost:8081/api/v1/requests/2/status | jq
```

**If you don't have `jq`, use:**
```bash
curl http://localhost:8081/api/v1/requests/2/status
```

**Expected Progression:**

**Time 0s (Immediately after creation):**
```json
{
  "status": "PENDING",  // ✅ Step 25: Initial status
  "tasks": [
    {
      "agent_type": "RESEARCHER",
      "status": "PENDING"
    }
  ]
}
```

**Time 5-10s (After RESEARCHER starts):**
```json
{
  "status": "PENDING",  // Still PENDING (RESEARCHER in progress)
  "tasks": [
    {
      "agent_type": "RESEARCHER",
      "status": "IN_PROGRESS"  // ✅ Task status updated
    }
  ]
}
```

**Time 10-15s (After RESEARCHER completes, WRITER starts):**
```json
{
  "status": "IN_PROGRESS",  // ✅ Step 25: Status updated when workflow starts
  "tasks": [
    {
      "agent_type": "RESEARCHER",
      "status": "COMPLETED"
    },
    {
      "agent_type": "WRITER",
      "status": "IN_PROGRESS"
    }
  ]
}
```

**Time 20-25s (After WRITER completes):**
```json
{
  "status": "IN_PROGRESS",  // Still IN_PROGRESS (no Publisher yet)
  "tasks": [
    {
      "agent_type": "RESEARCHER",
      "status": "COMPLETED"
    },
    {
      "agent_type": "WRITER",
      "status": "COMPLETED"
    }
  ]
}
```

---

## STEP 9: Verify in Database (Optional but Recommended)

### Check Content Requests Table

```bash
docker exec growth-engine-postgres psql -U admin -d growthengine -c "SELECT id, topic, status, created_at FROM content_requests ORDER BY id;"
```

**Expected Output:**
```
 id |              topic               |   status    |      created_at
----+----------------------------------+-------------+---------------------
  1 | Artificial Intelligence...      | IN_PROGRESS | 2024-01-15 10:30:00
  2 | Machine Learning Basics         | IN_PROGRESS | 2024-01-15 10:35:00
```

**What to Verify:**
- ✅ Status is `PENDING` initially, then changes to `IN_PROGRESS`
- ✅ Status updates are persisted in database

---

### Check Tasks Table

```bash
docker exec growth-engine-postgres psql -U admin -d growthengine -c "SELECT id, request_id, agent_type, status, created_at FROM tasks ORDER BY request_id, id;"
```

**Expected Output:**
```
 id | request_id | agent_type |   status    |      created_at
----+------------+------------+-------------+---------------------
  1 |          1 | RESEARCHER | COMPLETED   | 2024-01-15 10:30:00
  2 |          1 | WRITER     | COMPLETED   | 2024-01-15 10:30:05
  3 |          2 | RESEARCHER | COMPLETED   | 2024-01-15 10:35:00
  4 |          2 | WRITER     | IN_PROGRESS | 2024-01-15 10:35:05
```

**What to Verify:**
- ✅ Tasks are created for each request
- ✅ Task statuses progress: PENDING → IN_PROGRESS → COMPLETED
- ✅ Tasks are linked to requests via `request_id`

---

## STEP 10: Test Error Cases

### Test 10.1: Request Not Found

```bash
curl http://localhost:8081/api/v1/requests/99999/status
```

**Expected:**
```json
{
  "error": "Request not found"
}
```

**HTTP Status:** 404 Not Found

---

### Test 10.2: Invalid Request ID

```bash
curl http://localhost:8081/api/v1/requests/abc/status
```

**Expected:** Spring Boot validation error (400 Bad Request)

---

## Troubleshooting

### Problem: Services Won't Start

**Check:**
1. Docker is running: `docker ps`
2. Ports are not in use: `lsof -i :8081`, `lsof -i :5432`, `lsof -i :5672`
3. Java version: `java -version` (should be 21)
4. Maven version: `mvn -version`

---

### Problem: Database Connection Error

**Check:**
1. PostgreSQL is running: `docker ps | grep postgres`
2. Can connect: `docker exec growth-engine-postgres psql -U admin -d growthengine -c "SELECT 1;"`
3. Check logs: `docker logs growth-engine-postgres`

---

### Problem: RabbitMQ Connection Error

**Check:**
1. RabbitMQ is running: `docker ps | grep rabbitmq`
2. Management UI: http://localhost:15672 (admin/admin123)
3. Check logs: `docker logs growth-engine-rabbitmq`

---

### Problem: Tasks Not Processing

**Check:**
1. All agents are running (Terminals 3, 4, 5)
2. Queues exist in RabbitMQ UI: http://localhost:15672 → Queues
3. Check agent logs for errors
4. Verify scheduler is running (check orchestrator logs for "📤 Triggered writer task")

---

### Problem: Status Not Updating

**Check:**
1. Scheduler is running (check orchestrator logs)
2. Tasks are completing (check database)
3. `areAllTasksCompleted()` method is working
4. Check for exceptions in orchestrator logs

---

## Success Criteria

✅ **Step 17 (Enhanced Status Endpoint):**
- Status endpoint returns request + tasks array
- Tasks show correct agent_type, status, timestamps
- All tasks for a request are included

✅ **Step 25 (Status Updates):**
- Request status starts as "PENDING"
- Request status changes to "IN_PROGRESS" when workflow starts
- Status changes are persisted in database
- Status updates happen automatically via scheduler

---

## Next Steps

Once testing is successful:
- ✅ Step 17: Complete
- ✅ Step 25: Complete
- 🎯 Ready for Step 18: Create Evaluator Agent

---

## Quick Test Script

Save this as `test-steps-17-25.sh`:

```bash
#!/bin/bash

echo "🧪 Testing Steps 17 & 25"
echo ""

# Create request
echo "1. Creating request..."
RESPONSE=$(curl -s -X POST http://localhost:8081/api/v1/requests \
  -H "Content-Type: application/json" \
  -d '{"topic": "Test Topic", "tone": "professional"}')

REQUEST_ID=$(echo $RESPONSE | grep -o '"request_id":[0-9]*' | grep -o '[0-9]*')
echo "✅ Created request ID: $REQUEST_ID"
echo ""

# Check status immediately
echo "2. Checking initial status (should be PENDING)..."
curl -s http://localhost:8081/api/v1/requests/$REQUEST_ID/status | jq '.status, .tasks'
echo ""

# Wait for processing
echo "3. Waiting 15 seconds for tasks to process..."
sleep 15

# Check status again
echo "4. Checking status after processing (should be IN_PROGRESS)..."
curl -s http://localhost:8081/api/v1/requests/$REQUEST_ID/status | jq '.status, .tasks'
echo ""

echo "✅ Test complete!"
```

**Make it executable:**
```bash
chmod +x test-steps-17-25.sh
./test-steps-17-25.sh
```

---

**Happy Testing! 🚀**

