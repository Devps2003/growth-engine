package com.growthengine.agent.writer.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class WriterService {
    
    /**
     * Generates content based on research data.
     * 
     * @param researchResult The research data from Researcher Agent
     * @param topic The topic for the content
     * @param tone The desired tone (professional, casual, etc.)
     * @return Generated content with title and body
     */
    public Map<String, Object> writeContent(Map<String, Object> researchResult, String topic, String tone) {
        // TODO: Later, integrate with LLM API (OpenAI, Anthropic, etc.)
        // For now, return mock content
        
        System.out.println("✍️ Writing content for topic: " + topic + " with tone: " + tone);
        
        Map<String, Object> content = new HashMap<>();
        content.put("title", "An Introduction to " + topic);
        content.put("body", 
            "This article explores " + topic + ". " +
            "Based on research findings, we can see that this is an important topic. " +
            "The research indicates several key points that are worth discussing. " +
            "\n\n" +
            "In conclusion, " + topic + " represents a significant area of study. " +
            "The information gathered provides valuable insights into this subject matter."
        );
        content.put("tone", tone);
        content.put("topic", topic);
        content.put("wordCount", 50); // Mock word count
        
        // Include research summary in meta
        if (researchResult != null && researchResult.containsKey("summary")) {
            content.put("researchSummary", researchResult.get("summary"));
        }
        
        return content;
    }
}

