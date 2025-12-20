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
        // Step 1: Try to find PUBLISHER task first (has published URL and final content)
        List<Task> publisherTasks = taskRepository.findByRequestIdAndAgentType(
            requestId, 
            AgentType.PUBLISHER
        );
        
        for (Task task : publisherTasks) {
            if (task.getStatus() == TaskStatus.COMPLETED && 
                task.getResult() != null && 
                !task.getResult().isEmpty()) {
                
                try {
                    // Deserialize publisher result (contains published URL, published content, etc.)
                    Map<String, Object> publisherResult = objectMapper.readValue(
                        task.getResult(),
                        new TypeReference<Map<String, Object>>() {}
                    );
                    return publisherResult;
                } catch (JsonProcessingException e) {
                    System.err.println("❌ Error parsing publisher content for request " + requestId + ": " + e.getMessage());
                }
            }
        }
        
        // Step 2: Fallback to WRITER task if no publisher task found
        List<Task> writerTasks = taskRepository.findByRequestIdAndAgentType(
            requestId, 
            AgentType.WRITER
        );
        
        for (Task task : writerTasks) {
            if (task.getStatus() == TaskStatus.COMPLETED && 
                task.getResult() != null && 
                !task.getResult().isEmpty()) {
                
                try {
                    // Deserialize the JSON result to Map
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
        
        // No completed task found
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
     * Processes completed writer tasks and triggers evaluator tasks.
     * This is called periodically by the scheduler to maintain the workflow.
     */
    @Transactional
    public void processCompletedWriterTasks() {
        // Find all completed writer tasks that don't have an evaluator task yet
        List<Task> completedWriterTasks = taskRepository.findByAgentTypeAndStatus(
            AgentType.WRITER, 
            TaskStatus.COMPLETED
        );
        
        for (Task writerTask : completedWriterTasks) {
            // Check if evaluator task already exists for this request
            List<Task> existingEvaluatorTasks = taskRepository.findByRequestIdAndAgentType(
                writerTask.getRequestId(), 
                AgentType.EVALUATOR
            );
            
            if (!existingEvaluatorTasks.isEmpty()) {
                // Evaluator task already exists, skip
                continue;
            }
            
            try {
                // Get the writer result (the generated content)
                String writerResultJson = writerTask.getResult();
                if (writerResultJson == null || writerResultJson.isEmpty()) {
                    System.out.println("⚠️ Writer task " + writerTask.getId() + " has no result, skipping");
                    continue;
                }
                
                // Deserialize writer result (the content)
                Map<String, Object> writerContent = objectMapper.readValue(
                    writerResultJson, 
                    new TypeReference<Map<String, Object>>() {}
                );
                
                // Get the original request
                ContentRequest request = contentRequestRepository.findById(writerTask.getRequestId())
                    .orElse(null);
                
                if (request == null) {
                    System.out.println("⚠️ Request " + writerTask.getRequestId() + " not found, skipping");
                    continue;
                }
                
                // Create evaluator task payload
                // Pass the writer content so evaluator can evaluate it
                Map<String, Object> evaluatorPayload = new HashMap<>();
                evaluatorPayload.put("content", writerContent); // The content to evaluate
                evaluatorPayload.put("topic", request.getTopic());
                evaluatorPayload.put("tone", request.getTone());
                
                // Convert payload to JSON string
                String payloadJson = objectMapper.writeValueAsString(evaluatorPayload);
                
                // Save evaluator task to database
                Task evaluatorTask = new Task(
                    writerTask.getRequestId(),
                    AgentType.EVALUATOR,
                    payloadJson
                );
                Task savedEvaluatorTask = taskRepository.save(evaluatorTask);
                
                // Create TaskDTO for RabbitMQ
                TaskDTO evaluatorTaskDTO = new TaskDTO(
                    writerTask.getRequestId(),
                    AgentType.EVALUATOR,
                    evaluatorPayload
                );
                evaluatorTaskDTO.setId(savedEvaluatorTask.getId());
                
                // Publish evaluator task to queue
                rabbitTemplate.convertAndSend(RabbitMQConfig.EVALUATOR_QUEUE, evaluatorTaskDTO);
                
                System.out.println("📤 Triggered evaluator task for request " + writerTask.getRequestId() + 
                    " after writer completion");
                
            } catch (Exception e) {
                System.err.println("❌ Error processing writer task " + writerTask.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Processes completed evaluator tasks and triggers SEO tasks.
     * This is called periodically by the scheduler to maintain the workflow.
     */
    @Transactional
    public void processCompletedEvaluatorTasks() {
        // Find all completed evaluator tasks that don't have an SEO task yet
        List<Task> completedEvaluatorTasks = taskRepository.findByAgentTypeAndStatus(
            AgentType.EVALUATOR, 
            TaskStatus.COMPLETED
        );
        
        for (Task evaluatorTask : completedEvaluatorTasks) {
            // Check if SEO task already exists for this request
            List<Task> existingSeoTasks = taskRepository.findByRequestIdAndAgentType(
                evaluatorTask.getRequestId(), 
                AgentType.SEO
            );
            
            if (!existingSeoTasks.isEmpty()) {
                // SEO task already exists, skip
                continue;
            }
            
            try {
                // Get the evaluator result (contains evaluation + content)
                String evaluatorResultJson = evaluatorTask.getResult();
                if (evaluatorResultJson == null || evaluatorResultJson.isEmpty()) {
                    System.out.println("⚠️ Evaluator task " + evaluatorTask.getId() + " has no result, skipping");
                    continue;
                }
                
                // Deserialize evaluator result
                Map<String, Object> evaluatorResult = objectMapper.readValue(
                    evaluatorResultJson, 
                    new TypeReference<Map<String, Object>>() {}
                );
                
                // Get the original request
                ContentRequest request = contentRequestRepository.findById(evaluatorTask.getRequestId())
                    .orElse(null);
                
                if (request == null) {
                    System.out.println("⚠️ Request " + evaluatorTask.getRequestId() + " not found, skipping");
                    continue;
                }
                
                // Extract content and evaluation from evaluator result
                // The evaluator result contains: evaluatedContent, overallScore, feedback, etc.
                Map<String, Object> content = null;
                Map<String, Object> evaluationResult = new HashMap<>();
                
                // Check if evaluator result has evaluatedContent
                if (evaluatorResult.containsKey("evaluatedContent")) {
                    content = (Map<String, Object>) evaluatorResult.get("evaluatedContent");
                } else {
                    // Fallback: use evaluator result as content
                    content = evaluatorResult;
                }
                
                // Extract evaluation metrics
                if (evaluatorResult.containsKey("overallScore")) {
                    evaluationResult.put("overallScore", evaluatorResult.get("overallScore"));
                }
                if (evaluatorResult.containsKey("readabilityScore")) {
                    evaluationResult.put("readabilityScore", evaluatorResult.get("readabilityScore"));
                }
                if (evaluatorResult.containsKey("grammarScore")) {
                    evaluationResult.put("grammarScore", evaluatorResult.get("grammarScore"));
                }
                if (evaluatorResult.containsKey("structureScore")) {
                    evaluationResult.put("structureScore", evaluatorResult.get("structureScore"));
                }
                if (evaluatorResult.containsKey("feedback")) {
                    evaluationResult.put("feedback", evaluatorResult.get("feedback"));
                }
                if (evaluatorResult.containsKey("recommendations")) {
                    evaluationResult.put("recommendations", evaluatorResult.get("recommendations"));
                }
                
                // Create SEO task payload
                // Pass both content and evaluation result to SEO agent
                Map<String, Object> seoPayload = new HashMap<>();
                seoPayload.put("content", content); // The original content from Writer
                seoPayload.put("evaluationResult", evaluationResult); // The evaluation metrics
                seoPayload.put("topic", request.getTopic());
                seoPayload.put("tone", request.getTone());
                seoPayload.put("language", request.getLanguage());
                
                // Convert payload to JSON string
                String payloadJson = objectMapper.writeValueAsString(seoPayload);
                
                // Save SEO task to database
                Task seoTask = new Task(
                    evaluatorTask.getRequestId(),
                    AgentType.SEO,
                    payloadJson
                );
                Task savedSeoTask = taskRepository.save(seoTask);
                
                // Create TaskDTO for RabbitMQ
                TaskDTO seoTaskDTO = new TaskDTO(
                    evaluatorTask.getRequestId(),
                    AgentType.SEO,
                    seoPayload
                );
                seoTaskDTO.setId(savedSeoTask.getId());
                
                // Publish SEO task to queue
                rabbitTemplate.convertAndSend(RabbitMQConfig.SEO_QUEUE, seoTaskDTO);
                
                System.out.println("📤 Triggered SEO task for request " + evaluatorTask.getRequestId() + 
                    " after evaluator completion");
                
            } catch (Exception e) {
                System.err.println("❌ Error processing evaluator task " + evaluatorTask.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Processes completed SEO tasks and triggers publisher tasks.
     * This is called periodically by the scheduler to maintain the workflow.
     */
    @Transactional
    public void processCompletedSeoTasks() {
        // Find all completed SEO tasks that don't have a publisher task yet
        List<Task> completedSeoTasks = taskRepository.findByAgentTypeAndStatus(
            AgentType.SEO, 
            TaskStatus.COMPLETED
        );
        
        for (Task seoTask : completedSeoTasks) {
            // Check if publisher task already exists for this request
            List<Task> existingPublisherTasks = taskRepository.findByRequestIdAndAgentType(
                seoTask.getRequestId(), 
                AgentType.PUBLISHER
            );
            
            if (!existingPublisherTasks.isEmpty()) {
                // Publisher task already exists, skip
                continue;
            }
            
            try {
                // Get the SEO result (contains SEO-optimized content)
                String seoResultJson = seoTask.getResult();
                if (seoResultJson == null || seoResultJson.isEmpty()) {
                    System.out.println("⚠️ SEO task " + seoTask.getId() + " has no result, skipping");
                    continue;
                }
                
                // Deserialize SEO result
                Map<String, Object> seoOptimizedContent = objectMapper.readValue(
                    seoResultJson, 
                    new TypeReference<Map<String, Object>>() {}
                );
                
                // Get the original request
                ContentRequest request = contentRequestRepository.findById(seoTask.getRequestId())
                    .orElse(null);
                
                if (request == null) {
                    System.out.println("⚠️ Request " + seoTask.getRequestId() + " not found, skipping");
                    continue;
                }
                
                // Create publisher task payload
                Map<String, Object> publisherPayload = new HashMap<>();
                publisherPayload.put("seoOptimizedContent", seoOptimizedContent); // The SEO-optimized content
                publisherPayload.put("topic", request.getTopic());
                publisherPayload.put("tone", request.getTone());
                publisherPayload.put("language", request.getLanguage());
                
                // Convert payload to JSON string
                String payloadJson = objectMapper.writeValueAsString(publisherPayload);
                
                // Save publisher task to database
                Task publisherTask = new Task(
                    seoTask.getRequestId(),
                    AgentType.PUBLISHER,
                    payloadJson
                );
                Task savedPublisherTask = taskRepository.save(publisherTask);
                
                // Create TaskDTO for RabbitMQ
                TaskDTO publisherTaskDTO = new TaskDTO(
                    seoTask.getRequestId(),
                    AgentType.PUBLISHER,
                    publisherPayload
                );
                publisherTaskDTO.setId(savedPublisherTask.getId());
                
                // Publish publisher task to queue
                rabbitTemplate.convertAndSend(RabbitMQConfig.PUBLISHER_QUEUE, publisherTaskDTO);
                
                System.out.println("📤 Triggered publisher task for request " + seoTask.getRequestId() + 
                    " after SEO completion");
                
            } catch (Exception e) {
                System.err.println("❌ Error processing SEO task " + seoTask.getId() + ": " + e.getMessage());
                e.printStackTrace();
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
     * 2. Process completed writer tasks and trigger evaluator tasks
     * 3. Process completed evaluator tasks and trigger SEO tasks (NEW - Step 21)
     * 4. Check if any requests are fully completed and update their status
     */
    @Scheduled(fixedDelay = 5000)
    public void scheduleTaskProcessing() {
        // Step 1: Process research tasks
        processCompletedResearchTasks();
        
        // Step 2: Process writer tasks
        processCompletedWriterTasks();
        
        // Step 3: Process evaluator tasks
        processCompletedEvaluatorTasks();
        
        // Step 4: Process SEO tasks (NEW - Step 23)
        processCompletedSeoTasks();
        
        // Step 5: Check all requests and update status if all tasks completed
        List<ContentRequest> inProgressRequests = contentRequestRepository.findAll()
            .stream()
            .filter(req -> "IN_PROGRESS".equals(req.getStatus()))
            .toList();
        
        for (ContentRequest request : inProgressRequests) {
            updateRequestStatusIfCompleted(request.getId());
        }
    }
    
}
