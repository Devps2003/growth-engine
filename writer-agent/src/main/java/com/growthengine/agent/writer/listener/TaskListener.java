package com.growthengine.agent.writer.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growthengine.agent.writer.entity.Task;
import com.growthengine.agent.writer.repository.TaskRepository;
import com.growthengine.agent.writer.service.WriterService;
import com.growthengine.common.dto.TaskDTO;
import com.growthengine.common.enums.TaskStatus;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class TaskListener {
    
    @Autowired
    private WriterService writerService;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @RabbitListener(queues = "${writer.queue.name}")
    public void handleTask(TaskDTO task) {
        System.out.println("📨 Writer received task: " + task.getId() + " for request: " + task.getRequestId());
        
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
            
            // Step 2: Extract data from payload
            Map<String, Object> payload = task.getPayload();
            String topic = (String) payload.get("topic");
            String tone = (String) payload.get("tone");
            
            // Step 3: Get research result from payload (research agent passes it along)
            Map<String, Object> researchResult = null;
            if (payload.containsKey("researchResult")) {
                Object researchObj = payload.get("researchResult");
                if (researchObj instanceof Map) {
                    researchResult = (Map<String, Object>) researchObj;
                } else if (researchObj instanceof String) {
                    // Deserialize if it's a JSON string
                    researchResult = objectMapper.readValue((String) researchObj, 
                        new TypeReference<Map<String, Object>>() {});
                }
            }
            
            // Step 4: Generate content
            Map<String, Object> content = writerService.writeContent(researchResult, topic, tone);
            System.out.println("✍️ Content generated: " + content.get("title"));
            
            // Step 5: Convert content to JSON string
            String resultJson = objectMapper.writeValueAsString(content);
            
            // Step 6: Update task with result and mark as COMPLETED
            dbTask.setResult(resultJson);
            dbTask.setStatus(TaskStatus.COMPLETED);
            taskRepository.save(dbTask);
            
            System.out.println("✅ Content saved to database for task ID: " + task.getId());
            
        } catch (Exception e) {
            System.err.println("❌ Error processing task: " + e.getMessage());
            e.printStackTrace();
            
            // Step 7: Update task status to FAILED on error
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

