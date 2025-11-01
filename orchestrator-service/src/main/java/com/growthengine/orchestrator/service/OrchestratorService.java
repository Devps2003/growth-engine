package com.growthengine.orchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growthengine.common.dto.ContentRequestDTO;
import com.growthengine.common.dto.TaskDTO;
import com.growthengine.common.enums.AgentType;
import com.growthengine.common.enums.TaskStatus;
import com.growthengine.orchestrator.config.RabbitMQConfig;
import com.growthengine.orchestrator.entity.ContentRequest;
import com.growthengine.orchestrator.entity.Task;
import com.growthengine.orchestrator.repository.ContentRequestRepository;
import com.growthengine.orchestrator.repository.TaskRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrchestratorService {

    @Autowired
    private ContentRequestRepository contentRequestRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public ContentRequest createContentRequest(ContentRequestDTO requestDTO) {

        ContentRequest request = new ContentRequest(
                requestDTO.getUserId(),
                requestDTO.getTopic(),
                requestDTO.getTone(),
                requestDTO.getLanguage()
        );

        ContentRequest savedRequest = contentRequestRepository.save(request);
        System.out.println("✅ Saved content request with ID: " + savedRequest.getId());

        // Step 2: Create a research task
        Map<String, Object> payload = new HashMap<>();
        payload.put("topic", savedRequest.getTopic());
        payload.put("tone", savedRequest.getTone());
        payload.put("language", savedRequest.getLanguage());

        // Convert payload to JSON string (for database storage)
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize task payload", e);
        }

        // Step 3: Save task to database
        Task task = new Task(
                savedRequest.getId(),
                AgentType.RESEARCHER,
                payloadJson
        );
        Task savedTask = taskRepository.save(task);
        System.out.println("✅ Saved task with ID: " + savedTask.getId() + " for request: " + savedRequest.getId());

        // Step 4: Create TaskDTO for RabbitMQ (with task ID included)
        TaskDTO researcherTask = new TaskDTO(
                savedRequest.getId(),
                AgentType.RESEARCHER,
                payload
        );
        researcherTask.setId(savedTask.getId()); // Include database ID

        // Step 5: Publish task to RabbitMQ
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.RESEARCH_QUEUE,
                researcherTask
        );
        System.out.println("📤 Published research task to queue for request ID: " + savedRequest.getId());

        return savedRequest;
    }

    public ContentRequest getContentRequest(Long requestId) {
        return contentRequestRepository.findById(requestId).orElse(null);
    }
    
    /**
     * Processes completed research tasks and triggers writer tasks.
     * This is called periodically by the scheduler to maintain the workflow.
     */
    @Transactional
    public void processCompletedResearchTasks() {
        // Find all completed research tasks that don't have a writer task yet
        List<Task> completedResearchTasks = taskRepository.findByAgentTypeAndStatus(
            AgentType.RESEARCHER, 
            TaskStatus.COMPLETED
        );
        
        for (Task researchTask : completedResearchTasks) {
            // Check if writer task already exists for this request
            List<Task> existingWriterTasks = taskRepository.findByRequestIdAndAgentType(
                researchTask.getRequestId(), 
                AgentType.WRITER
            );
            
            if (!existingWriterTasks.isEmpty()) {
                // Writer task already exists, skip
                continue;
            }
            
            try {
                // Get the research result
                String researchResultJson = researchTask.getResult();
                if (researchResultJson == null || researchResultJson.isEmpty()) {
                    System.out.println("⚠️ Research task " + researchTask.getId() + " has no result, skipping");
                    continue;
                }
                
                // Deserialize research result
                Map<String, Object> researchResult = objectMapper.readValue(
                    researchResultJson, 
                    new TypeReference<Map<String, Object>>() {}
                );
                
                // Get the original request
                ContentRequest request = contentRequestRepository.findById(researchTask.getRequestId())
                    .orElse(null);
                
                if (request == null) {
                    System.out.println("⚠️ Request " + researchTask.getRequestId() + " not found, skipping");
                    continue;
                }
                
                // Create writer task payload
                Map<String, Object> writerPayload = new HashMap<>();
                writerPayload.put("topic", request.getTopic());
                writerPayload.put("tone", request.getTone());
                writerPayload.put("language", request.getLanguage());
                writerPayload.put("researchResult", researchResult); // Pass research results
                
                // Convert payload to JSON string
                String payloadJson = objectMapper.writeValueAsString(writerPayload);
                
                // Save writer task to database
                Task writerTask = new Task(
                    researchTask.getRequestId(),
                    AgentType.WRITER,
                    payloadJson
                );
                Task savedWriterTask = taskRepository.save(writerTask);
                
                // Create TaskDTO for RabbitMQ
                TaskDTO writerTaskDTO = new TaskDTO(
                    researchTask.getRequestId(),
                    AgentType.WRITER,
                    writerPayload
                );
                writerTaskDTO.setId(savedWriterTask.getId());
                
                // Publish writer task to queue
                rabbitTemplate.convertAndSend(RabbitMQConfig.WRITER_QUEUE, writerTaskDTO);
                
                System.out.println("📤 Triggered writer task for request " + researchTask.getRequestId() + 
                    " after research completion");
                
                // Update request status
                request.setStatus("IN_PROGRESS");
                contentRequestRepository.save(request);
                
            } catch (Exception e) {
                System.err.println("❌ Error processing research task " + researchTask.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Scheduled job that runs every 5 seconds to check for completed research tasks
     * and trigger writer tasks.
     */
    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    public void scheduleTaskProcessing() {
        processCompletedResearchTasks();
    }
    
}
