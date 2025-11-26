#!/bin/bash

echo "🧪 Testing Growth Engine Workflow"
echo "=================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if orchestrator is running
echo "1. Checking if services are running..."
if ! curl -s http://localhost:8081/api/v1/requests/999/status > /dev/null 2>&1; then
    echo -e "${RED}   ❌ Orchestrator service is not running!${NC}"
    echo "   Please run ./start.sh first"
    exit 1
fi
echo -e "${GREEN}   ✅ Orchestrator service is running${NC}"
echo ""

# Step 2: Create a test request
echo "2. Creating a test content request..."
RESPONSE=$(curl -s -X POST http://localhost:8081/api/v1/requests \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Artificial Intelligence in Healthcare",
    "tone": "professional",
    "language": "English",
    "user_id": 1
  }')

# Extract request ID
REQUEST_ID=$(echo $RESPONSE | grep -o '"request_id":[0-9]*' | grep -o '[0-9]*')

if [ -z "$REQUEST_ID" ]; then
    echo -e "${RED}   ❌ Failed to create request${NC}"
    echo "   Response: $RESPONSE"
    exit 1
fi

echo -e "${GREEN}   ✅ Request created with ID: $REQUEST_ID${NC}"
echo "   Response: $RESPONSE"
echo ""

# Step 3: Check initial status
echo "3. Checking initial status (should be PENDING)..."
STATUS_RESPONSE=$(curl -s http://localhost:8081/api/v1/requests/$REQUEST_ID/status)
echo "$STATUS_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$STATUS_RESPONSE"
echo ""

# Step 4: Wait and monitor workflow
echo "4. Monitoring workflow progress..."
echo "   (This will check status every 5 seconds for 60 seconds)"
echo ""

for i in {1..12}; do
    echo -e "${BLUE}   Check #$i (after $((i*5)) seconds)...${NC}"
    
    STATUS_RESPONSE=$(curl -s http://localhost:8081/api/v1/requests/$REQUEST_ID/status)
    
    # Extract status
    STATUS=$(echo "$STATUS_RESPONSE" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    
    # Count tasks
    TASK_COUNT=$(echo "$STATUS_RESPONSE" | grep -o '"agent_type"' | wc -l | tr -d ' ')
    
    echo "   Status: $STATUS"
    echo "   Tasks: $TASK_COUNT"
    
    # Show task details
    if echo "$STATUS_RESPONSE" | grep -q "tasks"; then
        echo "$STATUS_RESPONSE" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if 'tasks' in data:
        for task in data['tasks']:
            print(f\"     - {task.get('agent_type', 'N/A')}: {task.get('status', 'N/A')}\")
except:
    pass
" 2>/dev/null || echo "     (Unable to parse tasks)"
    fi
    
    # Check if all tasks are completed
    if [ "$STATUS" = "COMPLETED" ] || [ "$STATUS" = "IN_PROGRESS" ]; then
        if echo "$STATUS_RESPONSE" | grep -q '"status":"COMPLETED"' | grep -q "tasks"; then
            ALL_COMPLETED=$(echo "$STATUS_RESPONSE" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if 'tasks' in data:
        all_done = all(task.get('status') == 'COMPLETED' for task in data['tasks'])
        print('true' if all_done else 'false')
except:
    print('false')
" 2>/dev/null)
            
            if [ "$ALL_COMPLETED" = "true" ]; then
                echo -e "${GREEN}   ✅ All tasks completed!${NC}"
                break
            fi
        fi
    fi
    
    echo ""
    sleep 5
done

echo ""

# Step 5: Final status check
echo "5. Final status check..."
FINAL_STATUS=$(curl -s http://localhost:8081/api/v1/requests/$REQUEST_ID/status)
echo "$FINAL_STATUS" | python3 -m json.tool 2>/dev/null || echo "$FINAL_STATUS"
echo ""

# Step 6: Try to get content
echo "6. Retrieving generated content..."
CONTENT_RESPONSE=$(curl -s http://localhost:8081/api/v1/requests/$REQUEST_ID/content)
if echo "$CONTENT_RESPONSE" | grep -q "content"; then
    echo -e "${GREEN}   ✅ Content retrieved successfully${NC}"
    echo "$CONTENT_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$CONTENT_RESPONSE"
else
    echo -e "${YELLOW}   ⚠️  Content not yet available or request still processing${NC}"
    echo "$CONTENT_RESPONSE"
fi
echo ""

# Step 7: Summary
echo "=================================="
echo -e "${GREEN}✅ Test complete!${NC}"
echo ""
echo "Request ID: $REQUEST_ID"
echo "View status: curl http://localhost:8081/api/v1/requests/$REQUEST_ID/status"
echo "View content: curl http://localhost:8081/api/v1/requests/$REQUEST_ID/content"
echo ""

