package com.growthengine.agent.researcher.listener;

import com.growthengine.agent.researcher.service.ResearchService;
import com.growthengine.common.dto.TaskDTO;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class TaskListener {
    
    @Autowired
    private ResearchService researchService;
    
    @RabbitListener(queues = "${researcher.queue.name}")
    public void handleTask(TaskDTO task) {
        System.out.println("📨 Received task: " + task.getId() + " for request: " + task.getRequestId());
        
        try {
            // Extract topic from payload
            String topic = (String) task.getPayload().get("topic");
            
            // Perform research
            Map<String, Object> result = researchService.performResearch(topic);
            
            // TODO: Later, we'll publish result back to orchestrator
            System.out.println("✅ Research completed: " + result);
            
        } catch (Exception e) {
            System.err.println("❌ Error processing task: " + e.getMessage());
            e.printStackTrace();
        }
    }
}