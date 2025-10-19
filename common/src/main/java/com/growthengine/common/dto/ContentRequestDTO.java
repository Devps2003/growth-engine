package com.growthengine.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContentRequestDTO {

    @JsonProperty("user_id")
    private Long userId;

    private String topic;
    private String tone;
    private String language;
    private String status;

    // Default constructor (required for Jackson)
    public ContentRequestDTO() {
    }

    // Constructor with fields
    public ContentRequestDTO(Long userId, String topic, String tone, String language) {
        this.userId = userId;
        this.topic = topic;
        this.tone = tone;
        this.language = language;
        this.status = "PENDING";
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}