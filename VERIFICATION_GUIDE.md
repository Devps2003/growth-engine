# 🔍 Integration Verification Guide

## Quick Verification Checklist

### ✅ Step 1: Check Services Are Running

```bash
# Check all services
./verify-integrations.sh

# Or manually:
lsof -i :8081  # Orchestrator
pgrep -f "researcher-agent"
pgrep -f "writer-agent"
```

---

### ✅ Step 2: Verify Groq API Key

**CRITICAL:** Writer agent needs Groq API key to work!

```bash
# Option A: Set environment variable (recommended)
export GROQ_API_KEY="your-actual-api-key-here"

# Option B: Update application.yml
# Edit: writer-agent/src/main/resources/application.yml
# Change: key: ${GROQ_API_KEY:your-api-key-here}
# To: key: your-actual-api-key-here
```

**Get API Key:**
1. Go to https://console.groq.com
2. Sign up/login
3. Create API key
4. Copy the key

**Verify it works:**
```bash
curl -X POST https://api.groq.com/openai/v1/chat/completions \
  -H "Authorization: Bearer $GROQ_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama-3.1-70b-versatile",
    "messages": [{"role": "user", "content": "Say hello"}],
    "max_tokens": 10
  }'
```

If you see `"choices"` in response → ✅ API key works!

---

### ✅ Step 3: Test Wikipedia API

Wikipedia API is free and doesn't need a key.

```bash
# Test direct API call
curl "https://en.wikipedia.org/api/rest_v1/page/summary/Artificial_intelligence"
```

If you get JSON response → ✅ Wikipedia API works!

**Note:** Wikipedia search might fail for complex topics. The system will use fallback data.

---

### ✅ Step 4: Test Complete Workflow

```bash
# 1. Create a test request
curl -X POST http://localhost:8081/api/v1/requests \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Python Programming",
    "tone": "professional"
  }'

# 2. Get the request ID from response
# 3. Check status
curl http://localhost:8081/api/v1/requests/{REQUEST_ID}/status

# 4. Monitor logs in real-time
tail -f logs/researcher-agent.log
tail -f logs/writer-agent.log
```

---

### ✅ Step 5: Verify API Calls in Logs

**Researcher Agent:**
```bash
tail -f logs/researcher-agent.log | grep -E "(Wikipedia|DuckDuckGo|✅|❌)"
```

**Expected output:**
- `📚 Searching Wikipedia for: [topic]`
- `✅ Wikipedia research completed` OR `⚠️ No Wikipedia page found`
- `🦆 Searching DuckDuckGo for: [topic]`
- `✅ DuckDuckGo search completed: X results`

**Writer Agent:**
```bash
tail -f logs/writer-agent.log | grep -E "(Groq|Writing|✅|❌)"
```

**Expected output:**
- `✍️ Writing content for topic: [topic]`
- `✅ Content generated successfully using Groq`
- OR `❌ Error calling Groq API` (if API key is wrong)

---

### ✅ Step 6: Check Database for Results

```bash
# Connect to PostgreSQL
docker exec -it growth-engine-postgres psql -U admin -d growthengine

# Check tasks
SELECT id, agent_type, status, created_at 
FROM tasks 
ORDER BY created_at DESC 
LIMIT 10;

# Check a specific task result
SELECT result::text 
FROM tasks 
WHERE id = <TASK_ID>;
```

---

## 🐛 Common Issues & Fixes

### Issue 1: Writer Agent Not Working

**Symptoms:**
- Writer tasks stay in PENDING status
- No Groq API calls in logs
- Writer agent logs show errors

**Fix:**
1. Set Groq API key:
   ```bash
   export GROQ_API_KEY="your-key-here"
   ```
2. Restart writer agent:
   ```bash
   pkill -f writer-agent
   cd writer-agent && mvn spring-boot:run
   ```

---

### Issue 2: Wikipedia Search Failing

**Symptoms:**
- `⚠️ No Wikipedia page found for: [topic]`
- Research still completes (using fallback)

**Why:**
- Wikipedia requires exact page title match
- Complex topics might not have exact matches

**Fix:**
- This is expected behavior - system uses fallback data
- For better results, use simpler, more common topics
- Example: "Python" instead of "Python Programming Language"

---

### Issue 3: DuckDuckGo Returning 0 Results

**Symptoms:**
- `✅ DuckDuckGo search completed: 0 results`

**Why:**
- DuckDuckGo HTML parsing might fail
- No official API, using HTML scraping

**Fix:**
- This is expected - DuckDuckGo integration is experimental
- System will still work with Wikipedia + fallback data

---

### Issue 4: Services Not Starting

**Symptoms:**
- Port conflicts
- Database connection errors

**Fix:**
```bash
# Stop all services
./shutdown.sh

# Check ports
lsof -i :8081
lsof -i :8080

# Restart Docker
docker-compose down
docker-compose up -d

# Start services
./start.sh
```

---

## 📊 Success Indicators

### ✅ All Integrations Working:

1. **Researcher Agent:**
   - ✅ Wikipedia API calls in logs
   - ✅ Research tasks completing
   - ✅ Real data in task results

2. **Writer Agent:**
   - ✅ Groq API calls in logs
   - ✅ Writer tasks completing
   - ✅ Real LLM-generated content

3. **Complete Workflow:**
   - ✅ Request → RESEARCHER → WRITER → EVALUATOR → SEO → PUBLISHER
   - ✅ All tasks completing
   - ✅ Request status = COMPLETED

---

## 🧪 Manual Testing Steps

### Test 1: Wikipedia Integration

```bash
# Watch researcher logs
tail -f logs/researcher-agent.log

# Create request with simple topic
curl -X POST http://localhost:8081/api/v1/requests \
  -H "Content-Type: application/json" \
  -d '{"topic": "Python", "tone": "professional"}'
```

**Expected:** See Wikipedia API calls in logs

---

### Test 2: Groq Integration

```bash
# Watch writer logs
tail -f logs/writer-agent.log

# Create request (after research completes)
# Writer agent should automatically process
```

**Expected:** See Groq API calls and generated content

---

### Test 3: End-to-End

```bash
# Create request
REQUEST_ID=$(curl -s -X POST http://localhost:8081/api/v1/requests \
  -H "Content-Type: application/json" \
  -d '{"topic": "Machine Learning", "tone": "professional"}' \
  | grep -o '"request_id":[0-9]*' | grep -o '[0-9]*')

# Monitor status
watch -n 2 "curl -s http://localhost:8081/api/v1/requests/$REQUEST_ID/status | python3 -m json.tool"

# Check final content
curl http://localhost:8081/api/v1/requests/$REQUEST_ID/content | python3 -m json.tool
```

---

## 📝 Verification Checklist

- [ ] All services running (orchestrator, researcher, writer)
- [ ] Groq API key configured and working
- [ ] Wikipedia API accessible
- [ ] Researcher agent making API calls
- [ ] Writer agent making Groq API calls
- [ ] Tasks completing successfully
- [ ] Real data in task results (not just mock)
- [ ] Complete workflow working (all 5 agents)

---

## 🎯 Next Steps After Verification

Once all integrations are verified:

1. ✅ **Writer Service** - Groq API working
2. ✅ **Research Service** - Wikipedia working
3. ⏭️ **Evaluation Service** - Add real algorithms (next step)
4. ⏭️ **SEO Service** - Add real SEO analysis
5. ⏭️ **Publisher Service** - Add CMS integration

---

## 📞 Need Help?

If integrations aren't working:

1. Check logs: `tail -f logs/*.log`
2. Verify API keys: `echo $GROQ_API_KEY`
3. Test APIs manually (see examples above)
4. Check service status: `./verify-integrations.sh`

