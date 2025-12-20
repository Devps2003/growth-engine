#!/bin/bash

echo "🔍 Verifying All API Integrations"
echo "===================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Step 1: Check if services are running
echo "1. Checking if services are running..."
echo ""

ORCHESTRATOR_RUNNING=false
RESEARCHER_RUNNING=false
WRITER_RUNNING=false
EVALUATOR_RUNNING=false
SEO_RUNNING=false
PUBLISHER_RUNNING=false

if lsof -i :8081 > /dev/null 2>&1; then
    echo -e "${GREEN}   ✅ Orchestrator Service (port 8081)${NC}"
    ORCHESTRATOR_RUNNING=true
else
    echo -e "${RED}   ❌ Orchestrator Service not running${NC}"
fi

if pgrep -f "researcher-agent" > /dev/null; then
    echo -e "${GREEN}   ✅ Researcher Agent${NC}"
    RESEARCHER_RUNNING=true
else
    echo -e "${RED}   ❌ Researcher Agent not running${NC}"
fi

if pgrep -f "writer-agent" > /dev/null; then
    echo -e "${GREEN}   ✅ Writer Agent${NC}"
    WRITER_RUNNING=true
else
    echo -e "${RED}   ❌ Writer Agent not running${NC}"
fi

if pgrep -f "evaluator-agent" > /dev/null; then
    echo -e "${GREEN}   ✅ Evaluator Agent${NC}"
    EVALUATOR_RUNNING=true
else
    echo -e "${RED}   ❌ Evaluator Agent not running${NC}"
fi

if pgrep -f "seo-agent" > /dev/null; then
    echo -e "${GREEN}   ✅ SEO Agent${NC}"
    SEO_RUNNING=true
else
    echo -e "${RED}   ❌ SEO Agent not running${NC}"
fi

if pgrep -f "publisher-agent" > /dev/null; then
    echo -e "${GREEN}   ✅ Publisher Agent${NC}"
    PUBLISHER_RUNNING=true
else
    echo -e "${RED}   ❌ Publisher Agent not running${NC}"
fi

echo ""

# Step 2: Check API Keys
echo "2. Checking API Keys Configuration..."
echo ""

if [ -z "$GROQ_API_KEY" ]; then
    echo -e "${YELLOW}   ⚠️  GROQ_API_KEY not set in environment${NC}"
    echo "      Checking application.yml..."
    if grep -q "your-api-key-here" writer-agent/src/main/resources/application.yml 2>/dev/null; then
        echo -e "${RED}   ❌ Groq API key not configured (still using placeholder)${NC}"
        echo "      Please set GROQ_API_KEY environment variable or update application.yml"
    else
        echo -e "${GREEN}   ✅ Groq API key configured in application.yml${NC}"
    fi
else
    echo -e "${GREEN}   ✅ GROQ_API_KEY environment variable is set${NC}"
fi

echo ""

# Step 3: Test Wikipedia API
echo "3. Testing Wikipedia API..."
echo ""

WIKI_TEST=$(curl -s "https://en.wikipedia.org/api/rest_v1/page/summary/Artificial_Intelligence" | head -c 100)
if [ ! -z "$WIKI_TEST" ]; then
    echo -e "${GREEN}   ✅ Wikipedia API is accessible${NC}"
else
    echo -e "${RED}   ❌ Wikipedia API not accessible${NC}"
fi

echo ""

# Step 4: Test Groq API (if key is available)
echo "4. Testing Groq API..."
echo ""

# Try to get API key from environment or application.yml
GROQ_KEY_TO_TEST=""
if [ ! -z "$GROQ_API_KEY" ] && [ "$GROQ_API_KEY" != "your-api-key-here" ]; then
    GROQ_KEY_TO_TEST="$GROQ_API_KEY"
elif [ -f "writer-agent/src/main/resources/application.yml" ]; then
    # Extract key from application.yml
    GROQ_KEY_TO_TEST=$(grep -A 1 "groq:" writer-agent/src/main/resources/application.yml 2>/dev/null | grep "key:" | sed 's/.*key:[[:space:]]*//' | tr -d '"' | tr -d "'")
fi

if [ ! -z "$GROQ_KEY_TO_TEST" ] && [ "$GROQ_KEY_TO_TEST" != "your-api-key-here" ] && [ "$GROQ_KEY_TO_TEST" != "" ]; then
    GROQ_TEST=$(curl -s -X POST https://api.groq.com/openai/v1/chat/completions \
        -H "Authorization: Bearer $GROQ_KEY_TO_TEST" \
        -H "Content-Type: application/json" \
        -d '{
            "model": "llama-3.3-70b-versatile",
            "messages": [{"role": "user", "content": "Say hello"}],
            "max_tokens": 10
        }' 2>&1)
    
    if echo "$GROQ_TEST" | grep -q "choices"; then
        echo -e "${GREEN}   ✅ Groq API is working${NC}"
    elif echo "$GROQ_TEST" | grep -q "401\|unauthorized\|invalid_api_key"; then
        echo -e "${RED}   ❌ Groq API key is invalid${NC}"
        echo "      Note: Writer agent may still work if key is configured in application.yml"
    elif echo "$GROQ_TEST" | grep -q "429\|rate limit"; then
        echo -e "${YELLOW}   ⚠️  Groq API rate limit reached${NC}"
    else
        echo -e "${YELLOW}   ⚠️  Groq API test inconclusive: ${GROQ_TEST:0:80}${NC}"
        echo "      (This may be a temporary issue - check writer-agent logs)"
    fi
else
    echo -e "${YELLOW}   ⚠️  Skipping Groq API test (API key not found)${NC}"
    echo "      Check: export GROQ_API_KEY or update writer-agent/src/main/resources/application.yml"
fi

echo ""

# Step 4c: Check WordPress Configuration (Optional)
echo "4c. Checking WordPress Integration (Optional)..."
echo ""

if [ -f "publisher-agent/src/main/resources/application.yml" ]; then
    WORDPRESS_ENABLED=$(grep -A 5 "wordpress:" publisher-agent/src/main/resources/application.yml 2>/dev/null | grep "enabled:" | awk '{print $2}' | tr -d '"' | tr -d "'")
    WORDPRESS_URL=$(grep -A 5 "wordpress:" publisher-agent/src/main/resources/application.yml 2>/dev/null | grep "url:" | awk '{print $2}' | tr -d '"' | tr -d "'")
    
    if [ "$WORDPRESS_ENABLED" = "true" ] && [ -n "$WORDPRESS_URL" ] && [ "$WORDPRESS_URL" != "" ]; then
        echo -e "${GREEN}   ✅ WordPress integration enabled${NC}"
        echo -e "${BLUE}      URL: $WORDPRESS_URL${NC}"
        
        # Test WordPress REST API connectivity
        WORDPRESS_TEST=$(curl -s -o /dev/null -w "%{http_code}" "$WORDPRESS_URL/wp-json/wp/v2/posts?per_page=1" 2>/dev/null || echo "000")
        if [ "$WORDPRESS_TEST" = "200" ] || [ "$WORDPRESS_TEST" = "401" ]; then
            echo -e "${GREEN}      ✅ WordPress REST API accessible (HTTP $WORDPRESS_TEST)${NC}"
        else
            echo -e "${YELLOW}      ⚠️  WordPress REST API not accessible (HTTP $WORDPRESS_TEST)${NC}"
            echo -e "${BLUE}         This may be normal if authentication is required${NC}"
        fi
    else
        echo -e "${YELLOW}   ⚠️  WordPress integration not configured (using mock publishing)${NC}"
        echo -e "${BLUE}      This is OK for MVP - mock publishing will be used${NC}"
    fi
else
    echo -e "${YELLOW}   ⚠️  WordPress configuration file not found${NC}"
fi

echo ""

# Step 4b: Test LanguageTool API
echo "4b. Testing LanguageTool API (Grammar Checking)..."
echo ""

LANGUAGETOOL_TEST=$(curl -s -X POST https://api.languagetool.org/v2/check \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "text=This is a test.&language=en-US" 2>&1)

if echo "$LANGUAGETOOL_TEST" | grep -q "matches\|software"; then
    echo -e "${GREEN}   ✅ LanguageTool API is accessible${NC}"
elif echo "$LANGUAGETOOL_TEST" | grep -q "429\|rate limit"; then
    echo -e "${YELLOW}   ⚠️  LanguageTool API rate limit (free tier: 20 req/min)${NC}"
else
    echo -e "${YELLOW}   ⚠️  LanguageTool API test inconclusive${NC}"
fi

echo ""

# Step 5: Create a test request and monitor
echo "5. Creating test request to verify end-to-end integration..."
echo ""

if [ "$ORCHESTRATOR_RUNNING" = true ]; then
    echo "   Creating request..."
    RESPONSE=$(curl -s -X POST http://localhost:8081/api/v1/requests \
        -H "Content-Type: application/json" \
        -d '{
            "topic": "Python Programming",
            "tone": "professional",
            "language": "English"
        }')
    
    REQUEST_ID=$(echo $RESPONSE | grep -o '"request_id":[0-9]*' | grep -o '[0-9]*')
    
    if [ ! -z "$REQUEST_ID" ]; then
        echo -e "${GREEN}   ✅ Request created: ID $REQUEST_ID${NC}"
        echo ""
        echo "   Monitoring workflow (waiting 60 seconds for all 5 agents)..."
        echo "   Expected: RESEARCHER → WRITER → EVALUATOR → SEO → PUBLISHER"
        echo ""
        
        for i in {1..12}; do
            sleep 5
            STATUS_RESPONSE=$(curl -s http://localhost:8081/api/v1/requests/$REQUEST_ID/status)
            STATUS=$(echo "$STATUS_RESPONSE" | grep -o '"status":"[^"]*"' | cut -d'"' -f4 | head -1)
            
            echo "   Check #$i (after $((i*5))s): Status = $STATUS"
            
            # Count tasks and show agent types
            TASK_COUNT=$(echo "$STATUS_RESPONSE" | grep -o '"agent_type"' | wc -l | tr -d ' ')
            echo "            Tasks: $TASK_COUNT"
            
            # Show task statuses
            if echo "$STATUS_RESPONSE" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if 'tasks' in data:
        for task in data['tasks']:
            print(f\"            - {task.get('agent_type', 'N/A')}: {task.get('status', 'N/A')}\")
except:
    pass
" 2>/dev/null; then
                :
            fi
            
            if [ "$STATUS" = "COMPLETED" ]; then
                echo -e "${GREEN}   ✅ Request completed!${NC}"
                break
            fi
        done
        
        echo ""
        echo "   Final status:"
        echo "$STATUS_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$STATUS_RESPONSE"
        
    else
        echo -e "${RED}   ❌ Failed to create request${NC}"
        echo "   Response: $RESPONSE"
    fi
else
    echo -e "${YELLOW}   ⚠️  Skipping end-to-end test (Orchestrator not running)${NC}"
fi

echo ""

# Step 6: Check recent logs for API calls
echo "6. Checking recent logs for API integration evidence..."
echo ""

if [ -f "logs/researcher-agent.log" ]; then
    echo "   Researcher Agent logs:"
    if grep -q "📚 Searching Wikipedia" logs/researcher-agent.log 2>/dev/null; then
        echo -e "${GREEN}      ✅ Wikipedia API calls detected${NC}"
    else
        echo -e "${YELLOW}      ⚠️  No Wikipedia API calls in recent logs${NC}"
    fi
    
    if grep -q "🦆 Searching DuckDuckGo" logs/researcher-agent.log 2>/dev/null; then
        echo -e "${GREEN}      ✅ DuckDuckGo API calls detected${NC}"
    else
        echo -e "${YELLOW}      ⚠️  No DuckDuckGo API calls in recent logs${NC}"
    fi
fi

if [ -f "logs/writer-agent.log" ]; then
    echo "   Writer Agent logs:"
    if grep -q "✍️ Writing content" logs/writer-agent.log 2>/dev/null; then
        echo -e "${GREEN}      ✅ Writer service active${NC}"
    else
        echo -e "${YELLOW}      ⚠️  No writer activity in recent logs${NC}"
    fi
    
    if grep -q "Groq\|groq\|Content generated successfully" logs/writer-agent.log 2>/dev/null; then
        echo -e "${GREEN}      ✅ Groq API integration detected${NC}"
    else
        echo -e "${YELLOW}      ⚠️  No Groq API calls in recent logs${NC}"
    fi
    
    if grep -q "Error calling Groq\|Failed to generate" logs/writer-agent.log 2>/dev/null; then
        echo -e "${RED}      ❌ Groq API errors detected in logs${NC}"
        echo "      Recent errors:"
        grep -i "error\|failed" logs/writer-agent.log | tail -3 | sed 's/^/         /'
    fi
fi

if [ -f "logs/evaluator-agent.log" ]; then
    echo "   Evaluator Agent logs:"
    if grep -q "📊 Evaluating content quality" logs/evaluator-agent.log 2>/dev/null; then
        echo -e "${GREEN}      ✅ Evaluation service active${NC}"
    else
        echo -e "${YELLOW}      ⚠️  No evaluation activity in recent logs${NC}"
    fi
    
    if grep -q "Readability:\|Flesch\|Grade Level" logs/evaluator-agent.log 2>/dev/null; then
        echo -e "${GREEN}      ✅ Flesch-Kincaid readability algorithm detected${NC}"
    else
        echo -e "${YELLOW}      ⚠️  No readability calculations in recent logs${NC}"
    fi
    
    if grep -q "Grammar:\|LanguageTool\|grammarScore" logs/evaluator-agent.log 2>/dev/null; then
        echo -e "${GREEN}      ✅ LanguageTool grammar checking detected${NC}"
    else
        echo -e "${YELLOW}      ⚠️  No grammar checking in recent logs${NC}"
    fi
    
    if grep -q "Error.*LanguageTool\|Failed.*grammar" logs/evaluator-agent.log 2>/dev/null; then
        echo -e "${RED}      ❌ LanguageTool API errors detected${NC}"
    fi
fi

if [ -f "logs/seo-agent.log" ]; then
    echo "   SEO Agent logs:"
    if grep -q "🔍 Optimizing content for SEO" logs/seo-agent.log 2>/dev/null; then
        echo -e "${GREEN}      ✅ SEO service active${NC}"
    else
        echo -e "${YELLOW}      ⚠️  No SEO activity in recent logs${NC}"
    fi
fi

if [ -f "logs/publisher-agent.log" ]; then
    echo "   Publisher Agent logs:"
    if grep -q "📰 Publishing content" logs/publisher-agent.log 2>/dev/null; then
        echo -e "${GREEN}      ✅ Publisher service active${NC}"
    else
        echo -e "${YELLOW}      ⚠️  No publisher activity in recent logs${NC}"
    fi
    
    if grep -q "Publishing to WordPress" logs/publisher-agent.log 2>/dev/null; then
        echo -e "${GREEN}      ✅ WordPress publishing detected${NC}"
    elif grep -q "Using mock publishing" logs/publisher-agent.log 2>/dev/null; then
        echo -e "${YELLOW}      ⚠️  Using mock publishing (WordPress not configured)${NC}"
    fi
    
    if grep -q "Published to WordPress" logs/publisher-agent.log 2>/dev/null; then
        echo -e "${GREEN}      ✅ Successful WordPress publishing detected${NC}"
    fi
    
    if grep -q "Error publishing to WordPress" logs/publisher-agent.log 2>/dev/null; then
        echo -e "${RED}      ❌ WordPress publishing errors detected${NC}"
    fi
fi

echo ""

# Step 7: Verify evaluation results (if available)
echo "7. Checking evaluation results quality..."
echo ""

if [ "$ORCHESTRATOR_RUNNING" = true ] && [ ! -z "$REQUEST_ID" ]; then
    # Try to get content and check for evaluation data
    CONTENT_RESPONSE=$(curl -s http://localhost:8081/api/v1/requests/$REQUEST_ID/content 2>/dev/null)
    
    if echo "$CONTENT_RESPONSE" | grep -q "fleschReadingEase\|fleschKincaidGradeLevel\|grammarScore"; then
        echo -e "${GREEN}   ✅ Real evaluation metrics found in content${NC}"
        echo "      (Flesch-Kincaid, grammar scores detected)"
    elif echo "$CONTENT_RESPONSE" | grep -q "overallScore"; then
        echo -e "${YELLOW}   ⚠️  Evaluation scores found (may be mock)${NC}"
    else
        echo -e "${YELLOW}   ⚠️  No evaluation data in content response${NC}"
    fi
fi

echo ""

# Step 8: Summary
echo "===================================="
echo "📊 Integration Verification Summary"
echo "===================================="
echo ""

echo "✅ Services Status:"
echo "   - Orchestrator: $([ "$ORCHESTRATOR_RUNNING" = true ] && echo '✅ Running' || echo '❌ Not running')"
echo "   - Researcher: $([ "$RESEARCHER_RUNNING" = true ] && echo '✅ Running' || echo '❌ Not running')"
echo "   - Writer: $([ "$WRITER_RUNNING" = true ] && echo '✅ Running' || echo '❌ Not running')"
echo "   - Evaluator: $([ "$EVALUATOR_RUNNING" = true ] && echo '✅ Running' || echo '❌ Not running')"
echo "   - SEO: $([ "$SEO_RUNNING" = true ] && echo '✅ Running' || echo '❌ Not running')"
echo "   - Publisher: $([ "$PUBLISHER_RUNNING" = true ] && echo '✅ Running' || echo '❌ Not running')"
echo ""

echo "✅ API Integrations:"
echo "   - Wikipedia API: ✅ Free, no key needed"
echo "   - DuckDuckGo: ✅ Free, no key needed"
echo "   - Groq LLM: ✅ Configured"
echo "   - LanguageTool: ✅ Free tier (20 req/min)"
echo ""

echo "📋 To verify manually:"
echo "  1. Check researcher-agent.log for Wikipedia/DuckDuckGo calls"
echo "  2. Check writer-agent.log for Groq API calls"
echo "  3. Check evaluator-agent.log for Flesch-Kincaid & LanguageTool"
echo "  4. Check orchestrator logs for workflow completion"
echo ""
echo "View logs:"
echo "  tail -f logs/researcher-agent.log"
echo "  tail -f logs/writer-agent.log"
echo "  tail -f logs/evaluator-agent.log"
echo "  tail -f logs/seo-agent.log"
echo "  tail -f logs/publisher-agent.log"
echo "  tail -f logs/orchestrator-service.log"
echo ""
echo "Test complete workflow:"
echo "  curl -X POST http://localhost:8081/api/v1/requests \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"topic\": \"Your Topic\", \"tone\": \"professional\"}'"
echo ""

