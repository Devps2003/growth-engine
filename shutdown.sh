#!/bin/bash

echo "🛑 Shutting down Growth Engine services..."
echo ""

# Step 1: Kill all Spring Boot services (Java processes)
echo "1. Stopping Spring Boot services..."

# Kill orchestrator service
echo "   - Stopping orchestrator-service..."
lsof -ti :8081 | xargs kill -9 2>/dev/null || echo "     ✅ No orchestrator running"

# Kill all Java processes related to our services
echo "   - Stopping all agent services..."
pkill -f "orchestrator-service" 2>/dev/null || true
pkill -f "researcher-agent" 2>/dev/null || true
pkill -f "writer-agent" 2>/dev/null || true
pkill -f "evaluator-agent" 2>/dev/null || true

# Wait a moment for processes to terminate
sleep 2

# Kill processes from PID files if they exist
if [ -d "logs" ]; then
    echo "   - Stopping services from PID files..."
    for pid_file in logs/*.pid; do
        if [ -f "$pid_file" ]; then
            pid=$(cat "$pid_file")
            if ps -p $pid > /dev/null 2>&1; then
                kill -9 $pid 2>/dev/null || true
                echo "     ✅ Stopped process $pid"
            fi
            rm -f "$pid_file"
        fi
    done
fi

# Verify no Java processes are running
JAVA_PROCESSES=$(ps aux | grep -E "(orchestrator|researcher|writer|evaluator)" | grep java | grep -v grep | wc -l | tr -d ' ')
if [ "$JAVA_PROCESSES" -eq 0 ]; then
    echo "     ✅ All Spring Boot services stopped"
else
    echo "     ⚠️  Some processes may still be running"
    ps aux | grep -E "(orchestrator|researcher|writer|evaluator)" | grep java | grep -v grep
fi

echo ""

# Step 2: Stop Docker containers
echo "2. Stopping Docker containers..."
docker-compose down

echo ""

# Step 3: Verify everything is stopped
echo "3. Verifying shutdown..."

# Check ports
echo "   - Checking ports..."
if lsof -i :8081 > /dev/null 2>&1; then
    echo "     ⚠️  Port 8081 still in use"
else
    echo "     ✅ Port 8081 is free"
fi

# Check Docker
if docker ps | grep -E "(postgres|rabbitmq)" > /dev/null 2>&1; then
    echo "     ⚠️  Some Docker containers still running"
    docker ps | grep -E "(postgres|rabbitmq)"
else
    echo "     ✅ All Docker containers stopped"
fi

echo ""
echo "✅ Shutdown complete!"
echo ""
echo "To start again:"
echo "  1. docker-compose up -d"
echo "  2. Start each service: mvn spring-boot:run"

