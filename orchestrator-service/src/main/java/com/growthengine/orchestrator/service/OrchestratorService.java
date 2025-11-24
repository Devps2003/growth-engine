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

import java.util.stream.Collectors;
import java.util.ArrayList;
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
        // Set initial status to PENDING
        savedRequest.setStatus("PENDING");
        savedRequest = contentRequestRepository.save(savedRequest);
        System.out.println("✅ Saved content request with ID: " + savedRequest.getId());

        // Step 2: Create a research task
        // Step 3: Save task to database
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
    
    public Map<String, Object> getGeneratedContent(Long requestId) {
        // Step 1: Find the WRITER task for this request
        List<Task> writerTasks = taskRepository.findByRequestIdAndAgentType(
            requestId, 
            AgentType.WRITER
        );
        
        // Step 2: Find a completed writer task
        for (Task task : writerTasks) {
            if (task.getStatus() == TaskStatus.COMPLETED && 
                task.getResult() != null && 
                !task.getResult().isEmpty()) {
                
                try {
                    // Step 3: Deserialize the JSON result to Map
                    Map<String, Object> content = objectMapper.readValue(
                        task.getResult(),
                        new TypeReference<Map<String, Object>>() {}
                    );
                    return content;
                } catch (JsonProcessingException e) {
                    System.err.println("❌ Error parsing content for request " + requestId + ": " + e.getMessage());
                    return null;
                }
            }
        }
        
        // No completed writer task found
        return null;
    }


    /**
     * Gets a request with all its associated tasks.
     * This method fetches the request and all tasks, then converts them to a Map
     * for easy JSON serialization in the API response.
     * 
     * @param requestId The request ID
     * @return Map containing request details and list of tasks, or null if request not found
     */
    public Map<String, Object> getRequestWithTasks(Long requestId) {
        ContentRequest request = contentRequestRepository.findById(requestId).orElse(null);
        if(request==null){
            return null;
        }

        List<Task> tasks = taskRepository.findByRequestId(requestId);

        List<Map<String, Object>> taskList = new ArrayList<>();
        for( Task task: tasks){
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("id", task.getId());
            taskMap.put("agent_type", task.getAgentType().toString());
            taskMap.put("status", task.getStatus().toString());
            taskMap.put("created_at", task.getCreatedAt());
            taskMap.put("updated_at", task.getUpdatedAt());
            taskList.add(taskMap);
        }

         Map<String, Object> response = new HashMap<>();
        response.put("request_id", request.getId());
        response.put("status", request.getStatus());
        response.put("topic", request.getTopic());
        response.put("tone", request.getTone());
        response.put("language", request.getLanguage());
        response.put("created_at", request.getCreatedAt());
        response.put("tasks", taskList);
        
        return response;
    }


    /**
     * Checks if all tasks for a request are completed.
     * This will be used later to set request status to COMPLETED
     * when all agents finish their work.
     * 
     * @param requestId The request ID
     * @return true if all tasks are completed, false otherwise
     */
    public boolean areAllTasksCompleted(Long requestId) {
        // Get all tasks for this request
        List<Task> tasks = taskRepository.findByRequestId(requestId);
        
        if (tasks.isEmpty()) {
            // No tasks yet, so not completed
            return false;
        }
        
        // Check if ALL tasks are completed
        for (Task task : tasks) {
            if (task.getStatus() != TaskStatus.COMPLETED) {
                // Found at least one task that's not completed
                return false;
            }
        }
        
        // All tasks are completed!
        return true;
    }

    /**
     * Updates request status to COMPLETED if all tasks are done.
     * This should be called periodically by the scheduler.
     * 
     * @param requestId The request ID
     */
    @Transactional
    public void updateRequestStatusIfCompleted(Long requestId) {
        // Check if all tasks are completed
        if (areAllTasksCompleted(requestId)) {
            ContentRequest request = contentRequestRepository.findById(requestId).orElse(null);
            
            if (request != null && !"COMPLETED".equals(request.getStatus())) {
                request.setStatus("COMPLETED");
                contentRequestRepository.save(request);
                System.out.println("✅ Updated request " + requestId + " status to COMPLETED");
            }
        }
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
                // Update request status to IN_PROGRESS when workflow starts
                // This happens when first task (RESEARCHER) completes and triggers next step
                if (!"IN_PROGRESS".equals(request.getStatus()) && !"COMPLETED".equals(request.getStatus())) {
                    request.setStatus("IN_PROGRESS");
                    contentRequestRepository.save(request);
                    System.out.println("📊 Updated request " + request.getId() + " status to IN_PROGRESS");
                }
                                
            } catch (Exception e) {
                System.err.println("❌ Error processing research task " + researchTask.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Scheduled job that runs every 5 seconds to:
     * 1. Process completed research tasks and trigger writer tasks
     * 2. Check if any requests are fully completed and update their status
     */
    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    public void scheduleTaskProcessing() {
        // Step 1: Process research tasks (existing functionality)
        processCompletedResearchTasks();
        
        // Step 2: Check all requests and update status if all tasks completed
        // Get all requests that are IN_PROGRESS
        List<ContentRequest> inProgressRequests = contentRequestRepository.findAll()
            .stream()
            .filter(req -> "IN_PROGRESS".equals(req.getStatus()))
            .toList();
        
        // For each IN_PROGRESS request, check if all tasks are done
        for (ContentRequest request : inProgressRequests) {
            updateRequestStatusIfCompleted(request.getId());
        }
    }
    
}
