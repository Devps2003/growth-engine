package com.growthengine.agent.writer.entity;

import com.growthengine.common.enums.AgentType;
import com.growthengine.common.enums.TaskStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "request_id", nullable = false)
    private Long requestId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false)
    private AgentType agentType;
    
    @Column(columnDefinition = "jsonb")
    private String payload;  // JSON string
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;
    
    @Column(columnDefinition = "jsonb")
    private String result;  // JSON string (task result)
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = TaskStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Default constructor
    public Task() {
    }
    
    // Constructor
    public Task(Long requestId, AgentType agentType, String payload) {
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
    
    public String getPayload() {
        return payload;
    }
    
    public void setPayload(String payload) {
        this.payload = payload;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

