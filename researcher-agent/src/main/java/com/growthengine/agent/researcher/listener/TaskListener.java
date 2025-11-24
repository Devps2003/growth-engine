 package com.growthengine.agent.researcher.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growthengine.agent.researcher.entity.Task;
import com.growthengine.agent.researcher.repository.TaskRepository;
import com.growthengine.agent.researcher.service.ResearchService;
import com.growthengine.common.dto.TaskDTO;
import com.growthengine.common.enums.TaskStatus;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class TaskListener {
    
    @Autowired
    private ResearchService researchService;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @RabbitListener(queues = "${researcher.queue.name}")
    public void handleTask(TaskDTO task) {
        System.out.println("📨 Received task: " + task.getId() + " for request: " + task.getRequestId());
        
        if (task.getId() == null) {
            System.err.println("❌ Task ID is null, cannot update database");
            return;
        }
        
        try {
            // Step 1: Load task from database and update status to IN_PROGRESS
            Task dbTask = taskRepository.findById(task.getId())
                .orElseThrow(() -> new RuntimeException("Task not found in database: " + task.getId()));
            
            dbTask.setStatus(TaskStatus.IN_PROGRESS);
            taskRepository.save(dbTask);
            System.out.println("🔄 Updated task status to IN_PROGRESS");
            
            // Step 2: Extract topic from payload
            String topic = (String) task.getPayload().get("topic");
            
            // Step 3: Perform research
            Map<String, Object> result = researchService.performResearch(topic);
            System.out.println("🔍 Research completed for topic: " + topic);
            
            // Step 4: Convert result to JSON string
            String resultJson = objectMapper.writeValueAsString(result);
            
            // Step 5: Update task with result and mark as COMPLETED
            dbTask.setResult(resultJson);
            dbTask.setStatus(TaskStatus.COMPLETED);
            taskRepository.save(dbTask);
            
            System.out.println("✅ Research results saved to database for task ID: " + task.getId());
            System.out.println("📊 Result summary: " + result.get("summary"));
            
        } catch (Exception e) {
            System.err.println("❌ Error processing task: " + e.getMessage());
            e.printStackTrace();
            
            // Step 6: Update task status to FAILED on error
            try {
                if (task.getId() != null) {
                    Task dbTask = taskRepository.findById(task.getId()).orElse(null);
                    if (dbTask != null) {
                        dbTask.setStatus(TaskStatus.FAILED);
                        taskRepository.save(dbTask);
                        System.out.println("⚠️ Task status updated to FAILED");
                    }
                }
            } catch (Exception ex) {
                System.err.println("❌ Failed to update task status to FAILED: " + ex.getMessage());
            }
        }
    }
}