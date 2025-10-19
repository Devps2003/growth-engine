package com.growthengine.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.growthengine.common.enums.AgentType;
import com.growthengine.common.enums.TaskStatus;
import java.util.Map;

public class TaskDTO {
    
    private Long id;
    
    @JsonProperty("request_id")
    private Long requestId;
    
    @JsonProperty("agent_type")
    private AgentType agentType;
    
    private Map<String, Object> payload;
    
    private TaskStatus status;
    
    private Map<String, Object> result;
    
    // Default constructor
    public TaskDTO() {
    }
    
    // Constructor
    public TaskDTO(Long requestId, AgentType agentType, Map<String, Object> payload) {
        this.requestId = requestId;
        this.agentType = agentType;
        this.payload = payload;
        this.status = TaskStatus.PENDING;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getRequestId() {
        return requestId;
    }
    
    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }
    
    public AgentType getAgentType() {
        return agentType;
    }
    
    public void setAgentType(AgentType agentType) {
        this.agentType = agentType;
    }
    
    public Map<String, Object> getPayload() {
        return payload;
    }
    
    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public Map<String, Object> getResult() {
        return result;
    }
    
    public void setResult(Map<String, Object> result) {
        this.result = result;
    }
}

