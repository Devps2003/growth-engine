package com.growthengine.agent.publisher.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growthengine.agent.publisher.entity.Task;
import com.growthengine.agent.publisher.repository.TaskRepository;
import com.growthengine.agent.publisher.service.PublishingService;
import com.growthengine.common.dto.TaskDTO;
import com.growthengine.common.enums.TaskStatus;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TaskListener {
    
    @Autowired
    private PublishingService publishingService;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Listens to the publisher queue and processes publishing tasks.
     */
    @RabbitListener(queues = "${publisher.queue.name}")
    public void handleTask(TaskDTO task) {
        System.out.println("📥 Received publishing task for request: " + task.getRequestId());
        
        try {
            // Step 1: Load task from database
            Task dbTask = taskRepository.findById(task.getId())
                .orElseThrow(() -> new RuntimeException("Task not found: " + task.getId()));
            
            // Step 2: Update status to IN_PROGRESS
            dbTask.setStatus(TaskStatus.IN_PROGRESS);
            taskRepository.save(dbTask);
            System.out.println("🔄 Updated task " + dbTask.getId() + " status to IN_PROGRESS");
            
            // Step 3: Extract data from payload
            Map<String, Object> payload = task.getPayload();
            if (payload == null) {
                throw new RuntimeException("Task payload is null");
            }
            
            // Get the SEO-optimized content
            Map<String, Object> seoOptimizedContent = null;
            if (payload.containsKey("seoOptimizedContent")) {
                seoOptimizedContent = (Map<String, Object>) payload.get("seoOptimizedContent");
            } else if (payload.containsKey("content")) {
                seoOptimizedContent = (Map<String, Object>) payload.get("content");
            } else {
                // Assume payload itself is the content
                seoOptimizedContent = payload;
            }
            
            if (seoOptimizedContent == null) {
                throw new RuntimeException("SEO-optimized content not found in task payload");
            }
            
            // Get topic
            String topic = (String) payload.getOrDefault("topic", 
                seoOptimizedContent.getOrDefault("topic", "Unknown Topic").toString());
            
            // Step 4: Publish the content
            Map<String, Object> publishingResult = publishingService.publishContent(
                seoOptimizedContent, 
                topic
            );
            
            // Step 5: Convert result to JSON string for database storage
            String resultJson = objectMapper.writeValueAsString(publishingResult);
            
            // Step 6: Update task with result and set status to COMPLETED
            dbTask.setResult(resultJson);
            dbTask.setStatus(TaskStatus.COMPLETED);
            taskRepository.save(dbTask);
            
            System.out.println("✅ Publishing completed for task " + dbTask.getId() + 
                " (URL: " + publishingResult.get("publishedUrl") + ")");
            
        } catch (Exception e) {
            System.err.println("❌ Error processing publishing task: " + e.getMessage());
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