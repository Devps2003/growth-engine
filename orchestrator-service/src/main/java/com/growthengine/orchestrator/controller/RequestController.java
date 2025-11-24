package com.growthengine.orchestrator.controller;

import com.growthengine.common.dto.ContentRequestDTO;
import com.growthengine.orchestrator.entity.ContentRequest;
import com.growthengine.orchestrator.service.OrchestratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/requests")
public class RequestController {

    @Autowired
    private OrchestratorService orchestratorService;

    /**
     * Endpoint to create a new content request.
     * <p>
     * POST /api/v1/requests
     * Body: { "topic": "...", "tone": "...", "language": "...", "user_id": 1 }
     *
     * @param requestDTO The content request data
     * @return Response with request ID and status
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRequest(@RequestBody ContentRequestDTO requestDTO) {

        try {
            // Validate input
            if (requestDTO.getTopic() == null || requestDTO.getTopic().trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Topic is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Set defaults if not provided
            if (requestDTO.getTone() == null) {
                requestDTO.setTone("professional");
            }
            if (requestDTO.getLanguage() == null) {
                requestDTO.setLanguage("English");
            }
            if (requestDTO.getUserId() == null) {
                requestDTO.setUserId(1L); // Default user for MVP
            }

            // Create the request
            ContentRequest savedRequest = orchestratorService.createContentRequest(requestDTO);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("request_id", savedRequest.getId());
            response.put("status", savedRequest.getStatus());
            response.put("topic", savedRequest.getTopic());
            response.put("message", "Content request created successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Endpoint to get request status with all tasks.
     * <p>
     * GET /api/v1/requests/{id}/status
     *
     * @param id The request ID
     * @return Response with request details and all tasks
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getRequestStatus(@PathVariable Long id) {

        // Step 1: Get request with all tasks using the new service method
        Map<String, Object> requestWithTasks = orchestratorService.getRequestWithTasks(id);

        // Step 2: Check if request exists
        if (requestWithTasks == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Request not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        // Step 3: Return the response (already contains request + tasks)
        return ResponseEntity.ok(requestWithTasks);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Map<String, Object>> getRequestContent(@PathVariable Long id) {

        // Step 1: Check if request exists
        ContentRequest request = orchestratorService.getContentRequest(id);

        if (request == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Request not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        // Step 2: Get the generated content
        Map<String, Object> content = orchestratorService.getGeneratedContent(id);

        if (content == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Content not yet generated. Please check the status endpoint.");
            errorResponse.put("request_id", id);
            errorResponse.put("status", request.getStatus());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        // Step 3: Build successful response
        Map<String, Object> response = new HashMap<>();
        response.put("request_id", id);
        response.put("status", request.getStatus());
        response.put("content", content); // The actual generated content
        response.put("topic", request.getTopic());

        return ResponseEntity.ok(response);
    }
}