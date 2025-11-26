package com.growthengine.agent.evaluator.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class EvaluationService {
    
    /**
     * Evaluates content quality based on various criteria.
     * 
     * @param content The content to evaluate (from Writer agent)
     * @return Evaluation result with scores and feedback
     */
    public Map<String, Object> evaluateContent(Map<String, Object> content) {
        // TODO: Later, integrate with real evaluation APIs (grammar checkers, readability analyzers, etc.)
        // For now, return mock evaluation
        
        System.out.println("📊 Evaluating content quality...");
        
        // Extract content details
        String title = (String) content.getOrDefault("title", "");
        String body = (String) content.getOrDefault("body", "");
        String tone = (String) content.getOrDefault("tone", "");
        
        // Mock evaluation scores (0-100)
        int readabilityScore = calculateReadabilityScore(body);
        int grammarScore = 85; // Mock grammar score
        int structureScore = calculateStructureScore(title, body);
        int overallScore = (readabilityScore + grammarScore + structureScore) / 3;
        
        // Build evaluation result
        Map<String, Object> evaluation = new HashMap<>();
        evaluation.put("overallScore", overallScore);
        evaluation.put("readabilityScore", readabilityScore);
        evaluation.put("grammarScore", grammarScore);
        evaluation.put("structureScore", structureScore);
        
        // Add feedback
        evaluation.put("feedback", new String[]{
            "Content is well-structured",
            "Good use of topic: " + content.getOrDefault("topic", "N/A"),
            "Tone is appropriate: " + tone,
            overallScore >= 80 ? "Content meets quality standards" : "Content needs improvement"
        });
        
        // Add recommendations
        evaluation.put("recommendations", new String[]{
            readabilityScore < 70 ? "Consider simplifying sentence structure" : "Readability is good",
            structureScore < 70 ? "Add more headings and subheadings" : "Structure is well-organized"
        });
        
        // Include original content for reference
        evaluation.put("evaluatedContent", content);
        
        return evaluation;
    }
    
    /**
     * Mock readability score calculation.
     * In real implementation, this would use readability formulas (Flesch-Kincaid, etc.)
     */
    private int calculateReadabilityScore(String body) {
        if (body == null || body.isEmpty()) {
            return 50;
        }
        
        // Simple mock: longer content with good word variety = higher score
        int wordCount = body.split("\\s+").length;
        int score = Math.min(95, 60 + (wordCount / 10)); // Base 60 + bonus for length
        
        return score;
    }
    
    /**
     * Mock structure score calculation.
     * In real implementation, this would analyze headings, paragraphs, etc.
     */
    private int calculateStructureScore(String title, String body) {
        int score = 70; // Base score
        
        // Bonus for having a title
        if (title != null && !title.isEmpty()) {
            score += 10;
        }
        
        // Bonus for body length
        if (body != null && body.length() > 200) {
            score += 10;
        }
        
        return Math.min(100, score);
    }
}