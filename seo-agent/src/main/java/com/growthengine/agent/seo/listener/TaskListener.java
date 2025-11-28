package com.growthengine.agent.seo.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growthengine.agent.seo.entity.Task;
import com.growthengine.agent.seo.repository.TaskRepository;
import com.growthengine.agent.seo.service.SeoService;
import com.growthengine.common.dto.TaskDTO;
import com.growthengine.common.enums.TaskStatus;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TaskListener {
    
    @Autowired
    private SeoService seoService;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Listens to the SEO queue and processes SEO optimization tasks.
     * This method is called automatically when a message arrives in the queue.
     * 
     * @param task The TaskDTO received from RabbitMQ
     */
    @RabbitListener(queues = "${seo.queue.name}")
    public void handleTask(TaskDTO task) {
        System.out.println("📥 Received SEO optimization task for request: " + task.getRequestId());
        
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
            
            // Get the content (from Writer agent)
            Map<String, Object> content = null;
            if (payload.containsKey("content")) {
                content = (Map<String, Object>) payload.get("content");
            } else if (payload.containsKey("writerResult")) {
                content = (Map<String, Object>) payload.get("writerResult");
            } else {
                // Assume payload itself is the content
                content = payload;
            }
            
            if (content == null) {
                throw new RuntimeException("Content not found in task payload");
            }
            
            // Get evaluation result (from Evaluator agent) - optional
            Map<String, Object> evaluationResult = null;
            if (payload.containsKey("evaluationResult")) {
                evaluationResult = (Map<String, Object>) payload.get("evaluationResult");
            }
            
            // Get topic
            String topic = (String) payload.getOrDefault("topic", 
                content.getOrDefault("topic", "Unknown Topic").toString());
            
            // Step 4: Optimize content for SEO
            Map<String, Object> seoOptimized = seoService.optimizeForSeo(
                content, 
                evaluationResult, 
                topic
            );
            
            // Step 5: Convert result to JSON string for database storage
            String resultJson = objectMapper.writeValueAsString(seoOptimized);
            
            // Step 6: Update task with result and set status to COMPLETED
            dbTask.setResult(resultJson);
            dbTask.setStatus(TaskStatus.COMPLETED);
            taskRepository.save(dbTask);
            
            System.out.println("✅ SEO optimization completed for task " + dbTask.getId() + 
                " (SEO Score: " + seoOptimized.get("seoScore") + ")");
            
        } catch (Exception e) {
            System.err.println("❌ Error processing SEO task: " + e.getMessage());
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