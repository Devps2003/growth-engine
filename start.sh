#!/bin/bash

echo "🚀 Starting Growth Engine - Multi-Agent System"
echo "================================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Step 1: Check if Docker is running
echo "1. Checking Docker..."
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}   ❌ Docker is not running. Please start Docker Desktop.${NC}"
    exit 1
fi
echo -e "${GREEN}   ✅ Docker is running${NC}"
echo ""

# Step 2: Start Docker containers (PostgreSQL + RabbitMQ)
echo "2. Starting Docker containers (PostgreSQL + RabbitMQ)..."
cd "$(dirname "$0")"

# Check if containers are already running
if docker ps | grep -q "growth-engine-postgres\|growth-engine-rabbitmq"; then
    echo -e "${YELLOW}   ⚠️  Some containers are already running${NC}"
    docker ps | grep -E "(postgres|rabbitmq)"
    read -p "   Do you want to restart them? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker-compose down
        docker-compose up -d
    else
        echo "   Using existing containers"
    fi
else
    docker-compose up -d
fi

# Wait for containers to be healthy
echo "   Waiting for containers to be ready..."
sleep 5

# Verify containers are running
if docker ps | grep -q "growth-engine-postgres" && docker ps | grep -q "growth-engine-rabbitmq"; then
    echo -e "${GREEN}   ✅ Docker containers started${NC}"
else
    echo -e "${RED}   ❌ Failed to start Docker containers${NC}"
    exit 1
fi
echo ""

# Step 3: Build the project
echo "3. Building project (this may take a minute)..."
mvn clean install -DskipTests > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}   ✅ Build successful${NC}"
else
    echo -e "${RED}   ❌ Build failed. Check errors above.${NC}"
    exit 1
fi
echo ""

# Step 4: Start services in background
echo "4. Starting Spring Boot services..."
echo ""

# Function to start a service
start_service() {
    local service_name=$1
    local service_dir=$2
    local log_file="logs/${service_name}.log"
    
    # Create logs directory if it doesn't exist
    mkdir -p logs
    
    echo "   Starting ${service_name}..."
    cd "$service_dir"
    nohup mvn spring-boot:run > "../${log_file}" 2>&1 &
    local pid=$!
    echo $pid > "../logs/${service_name}.pid"
    cd ..
    
    # Wait a bit for service to start
    sleep 3
    
    # Check if process is still running
    if ps -p $pid > /dev/null 2>&1; then
        echo -e "${GREEN}   ✅ ${service_name} started (PID: $pid)${NC}"
        echo "      Logs: logs/${service_name}.log"
    else
        echo -e "${RED}   ❌ ${service_name} failed to start. Check logs/${service_name}.log${NC}"
    fi
}

# Start orchestrator service
start_service "orchestrator-service" "orchestrator-service"
sleep 2

# Start researcher agent
start_service "researcher-agent" "researcher-agent"
sleep 2

# Start writer agent
start_service "writer-agent" "writer-agent"
sleep 2

# Start evaluator agent
start_service "evaluator-agent" "evaluator-agent"
sleep 3

echo ""

# Step 5: Verify services are running
echo "5. Verifying services..."
echo ""

# Check orchestrator (port 8081)
if lsof -i :8081 > /dev/null 2>&1; then
    echo -e "${GREEN}   ✅ Orchestrator service is running on port 8081${NC}"
else
    echo -e "${RED}   ❌ Orchestrator service is not running${NC}"
fi

# Check if services are responding
sleep 2
if curl -s http://localhost:8081/api/v1/requests/999/status > /dev/null 2>&1; then
    echo -e "${GREEN}   ✅ Orchestrator API is responding${NC}"
else
    echo -e "${YELLOW}   ⚠️  Orchestrator API not responding yet (may need a few more seconds)${NC}"
fi

echo ""

# Step 6: Display status
echo "================================================"
echo -e "${GREEN}✅ All services started!${NC}"
echo ""
echo "Services running:"
echo "  • Orchestrator Service: http://localhost:8081"
echo "  • Researcher Agent: Running"
echo "  • Writer Agent: Running"
echo "  • Evaluator Agent: Running"
echo "  • PostgreSQL: localhost:5432"
echo "  • RabbitMQ Management: http://localhost:15672 (admin/admin123)"
echo ""
echo "Logs location:"
echo "  • logs/orchestrator-service.log"
echo "  • logs/researcher-agent.log"
echo "  • logs/writer-agent.log"
echo "  • logs/evaluator-agent.log"
echo ""
echo "To stop all services, run: ./shutdown.sh"
echo ""
echo "Test the API:"
echo "  curl -X POST http://localhost:8081/api/v1/requests \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"topic\": \"AI in Healthcare\", \"tone\": \"professional\"}'"
echo ""

