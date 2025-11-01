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
     * 
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
     * Endpoint to get request status.
     * 
     * GET /api/v1/requests/{id}/status
     * 
     * @param id The request ID
     * @return Response with request details
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getRequestStatus(@PathVariable Long id) {
        
        ContentRequest request = orchestratorService.getContentRequest(id);
        
        if (request == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Request not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("request_id", request.getId());
        response.put("status", request.getStatus());
        response.put("topic", request.getTopic());
        response.put("created_at", request.getCreatedAt());
        
        return ResponseEntity.ok(response);
    }
}