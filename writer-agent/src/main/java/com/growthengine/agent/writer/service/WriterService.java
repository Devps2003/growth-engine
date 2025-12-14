package com.growthengine.agent.writer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WriterService {
    
    @Autowired
    private GroqClient groqClient;
    
    /**
     * Generates content based on research data using Groq LLM.
     * 
     * @param researchResult The research data from Researcher Agent
     * @param topic The topic for the content
     * @param tone The desired tone (professional, casual, etc.)
     * @return Generated content with title and body
     */
    public Map<String, Object> writeContent(Map<String, Object> researchResult, String topic, String tone) {
        System.out.println("✍️ Writing content for topic: " + topic + " with tone: " + tone);
        
        try {
            // Build prompt for LLM
            String prompt = buildPrompt(researchResult, topic, tone);
            
            // Generate content using Groq
            String generatedText = groqClient.generateContent(prompt);
            
            // Parse and structure the response
            Map<String, Object> content = parseGeneratedContent(generatedText, topic, tone, researchResult);
            
            System.out.println("✅ Content generated successfully using Groq");
            return content;
            
        } catch (Exception e) {
            System.err.println("❌ Error generating content: " + e.getMessage());
            // Fallback to mock content if API fails
            return generateMockContent(topic, tone, researchResult);
        }
    }
    
    /**
     * Builds a prompt for the LLM based on research and requirements.
     */
    private String buildPrompt(Map<String, Object> researchResult, String topic, String tone) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert content writer. Write a high-quality article on the topic: ").append(topic).append("\n\n");
        
        // Add research information
        if (researchResult != null) {
            prompt.append("Research Summary:\n");
            if (researchResult.containsKey("summary")) {
                prompt.append(researchResult.get("summary")).append("\n\n");
            }
            if (researchResult.containsKey("keyPoints")) {
                prompt.append("Key Points:\n");
                Object keyPointsObj = researchResult.get("keyPoints");
                if (keyPointsObj instanceof List) {
                    List<?> keyPoints = (List<?>) keyPointsObj;
                    for (Object point : keyPoints) {
                        prompt.append("- ").append(point).append("\n");
                    }
                } else if (keyPointsObj instanceof Object[]) {
                    Object[] keyPoints = (Object[]) keyPointsObj;
                    for (Object point : keyPoints) {
                        prompt.append("- ").append(point).append("\n");
                    }
                }
                prompt.append("\n");
            }
        }
        
        // Add tone requirement
        prompt.append("Tone: ").append(tone).append("\n\n");
        
        // Add instructions
        prompt.append("Requirements:\n");
        prompt.append("1. Write a compelling title (50-60 characters)\n");
        prompt.append("2. Write a comprehensive article body (minimum 500 words)\n");
        prompt.append("3. Use the research information provided\n");
        prompt.append("4. Maintain a ").append(tone).append(" tone throughout\n");
        prompt.append("5. Structure the content with clear paragraphs\n\n");
        
        prompt.append("Format your response as:\n");
        prompt.append("TITLE: [your title here]\n");
        prompt.append("BODY: [your article body here]\n");
        
        return prompt.toString();
    }
    
    /**
     * Parses the LLM response into structured content.
     */
    private Map<String, Object> parseGeneratedContent(String generatedText, String topic, String tone, Map<String, Object> researchResult) {
        Map<String, Object> content = new HashMap<>();
        
        // Extract title
        String title = extractTitle(generatedText, topic);
        content.put("title", title);
        
        // Extract body
        String body = extractBody(generatedText);
        content.put("body", body);
        
        // Add metadata
        content.put("tone", tone);
        content.put("topic", topic);
        content.put("wordCount", countWords(body));
        
        // Include research summary
        if (researchResult != null && researchResult.containsKey("summary")) {
            content.put("researchSummary", researchResult.get("summary"));
        }
        
        return content;
    }
    
    /**
     * Extracts title from LLM response.
     */
    private String extractTitle(String text, String topic) {
        // Look for "TITLE:" marker
        if (text.contains("TITLE:")) {
            String[] parts = text.split("TITLE:");
            if (parts.length > 1) {
                String titlePart = parts[1].split("BODY:")[0].trim();
                if (!titlePart.isEmpty()) {
                    return titlePart;
                }
            }
        }
        
        // Fallback: use topic
        return "An Introduction to " + topic;
    }
    
    /**
     * Extracts body from LLM response.
     */
    private String extractBody(String text) {
        // Look for "BODY:" marker
        if (text.contains("BODY:")) {
            String[] parts = text.split("BODY:");
            if (parts.length > 1) {
                return parts[1].trim();
            }
        }
        
        // Fallback: use entire text
        return text.trim();
    }
    
    /**
     * Counts words in text.
     */
    private int countWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.split("\\s+").length;
    }
    
    /**
     * Fallback mock content generator (if API fails).
     */
    private Map<String, Object> generateMockContent(String topic, String tone, Map<String, Object> researchResult) {
        System.out.println("⚠️ Using fallback mock content");
        Map<String, Object> content = new HashMap<>();
        content.put("title", "An Introduction to " + topic);
        content.put("body", "This article explores " + topic + ". Based on research findings...");
        content.put("tone", tone);
        content.put("topic", topic);
        content.put("wordCount", 50);
        return content;
    }
}