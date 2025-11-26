package com.growthengine.agent.evaluator.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growthengine.agent.evaluator.entity.Task;
import com.growthengine.agent.evaluator.repository.TaskRepository;
import com.growthengine.agent.evaluator.service.EvaluationService;
import com.growthengine.common.dto.TaskDTO;
import com.growthengine.common.enums.TaskStatus;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TaskListener {
    
    @Autowired
    private EvaluationService evaluationService;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Listens to the evaluator queue and processes evaluation tasks.
     * This method is called automatically when a message arrives in the queue.
     * 
     * @param task The TaskDTO received from RabbitMQ
     */
    @RabbitListener(queues = "${evaluator.queue.name}")
    public void handleTask(TaskDTO task) {
        System.out.println("📥 Received evaluation task for request: " + task.getRequestId());
        
        try {
            // Step 1: Load task from database
            Task dbTask = taskRepository.findById(task.getId())
                .orElseThrow(() -> new RuntimeException("Task not found: " + task.getId()));
            
            // Step 2: Update status to IN_PROGRESS
            dbTask.setStatus(TaskStatus.IN_PROGRESS);
            taskRepository.save(dbTask);
            System.out.println("🔄 Updated task " + dbTask.getId() + " status to IN_PROGRESS");
            
            // Step 3: Extract content from payload
            // The payload should contain the content from Writer agent
            Map<String, Object> payload = task.getPayload();
            if (payload == null) {
                throw new RuntimeException("Task payload is null");
            }
            
            // Get the content (it might be nested in payload)
            Map<String, Object> content = null;
            if (payload.containsKey("content")) {
                // Content is directly in payload
                content = (Map<String, Object>) payload.get("content");
            } else if (payload.containsKey("writerResult")) {
                // Content is in writerResult field
                content = (Map<String, Object>) payload.get("writerResult");
            } else {
                // Assume payload itself is the content
                content = payload;
            }
            
            if (content == null) {
                throw new RuntimeException("Content not found in task payload");
            }
            
            // Step 4: Evaluate the content
            Map<String, Object> evaluationResult = evaluationService.evaluateContent(content);
            
            // Step 5: Convert result to JSON string for database storage
            String resultJson = objectMapper.writeValueAsString(evaluationResult);
            
            // Step 6: Update task with result and set status to COMPLETED
            dbTask.setResult(resultJson);
            dbTask.setStatus(TaskStatus.COMPLETED);
            taskRepository.save(dbTask);
            
            System.out.println("✅ Evaluation completed for task " + dbTask.getId() + 
                " (Score: " + evaluationResult.get("overallScore") + ")");
            
        } catch (Exception e) {
            System.err.println("❌ Error processing evaluation task: " + e.getMessage());
            e.printStackTrace();
            
            // Update task status to FAILED
            try {
                Task dbTask = taskRepository.findById(task.getId()).orElse(null);
                if (dbTask != null) {
                    dbTask.setStatus(TaskStatus.FAILED);
                    taskRepository.save(dbTask);
                }
            } catch (Exception ex) {
                System.err.println("Failed to update task status to FAILED: " + ex.getMessage());
            }
        }
    }
}