package com.growthengine.agent.writer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GroqClient {
    
    @Value("${groq.api.key}")
    private String apiKey;
    
    @Value("${groq.api.base-url}")
    private String baseUrl;
    
    @Value("${groq.api.model}")
    private String model;
    
    @Value("${groq.api.max-tokens:2000}")
    private int maxTokens;
    
    @Value("${groq.api.temperature:0.7}")
    private double temperature;
    
    private WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public GroqClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .build();
    }
    
    /**
     * Generates content using Groq API.
     * 
     * @param prompt The prompt to send to the LLM
     * @return Generated text content
     */
    public String generateContent(String prompt) {
        try {
            // Build request payload
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            
            // Make API call
            Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block(); // Block for now (can make async later)
            
            // Extract content from response
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            
            throw new RuntimeException("No content in Groq response");
            
        } catch (Exception e) {
            System.err.println("❌ Error calling Groq API: " + e.getMessage());
            throw new RuntimeException("Failed to generate content: " + e.getMessage(), e);
        }
    }
}