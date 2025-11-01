package com.growthengine.orchestrator.repository;

import com.growthengine.orchestrator.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    // Find all tasks for a specific request
    List<Task> findByRequestId(Long requestId);
    
    // Find tasks by request ID and agent type
    List<Task> findByRequestIdAndAgentType(Long requestId, com.growthengine.common.enums.AgentType agentType);
    
    // Find tasks by status
    List<Task> findByStatus(com.growthengine.common.enums.TaskStatus status);
    
    // Find tasks by agent type and status
    List<Task> findByAgentTypeAndStatus(
        com.growthengine.common.enums.AgentType agentType,
        com.growthengine.common.enums.TaskStatus status
    );
}