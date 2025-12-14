package com.growthengine.agent.evaluator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class EvaluationService {
    
    @Autowired
    private ReadabilityCalculator readabilityCalculator;
    
    @Autowired
    private LanguageToolClient languageToolClient;
    
    /**
     * Evaluates content quality using real algorithms.
     * 
     * @param content The content to evaluate (from Writer agent)
     * @return Evaluation result with scores and feedback
     */
    public Map<String, Object> evaluateContent(Map<String, Object> content) {
        System.out.println("📊 Evaluating content quality...");
        
        // Extract content details
        String title = (String) content.getOrDefault("title", "");
        String body = (String) content.getOrDefault("body", "");
        String tone = (String) content.getOrDefault("tone", "");
        String language = (String) content.getOrDefault("language", "en-US");
        
        // Step 1: Calculate readability using Flesch-Kincaid
        double fleschScore = readabilityCalculator.calculateFleschReadingEase(body);
        double gradeLevel = readabilityCalculator.calculateFleschKincaidGradeLevel(body);
        int readabilityScore = readabilityCalculator.convertToReadabilityScore(fleschScore);
        
        System.out.println("   📖 Readability: " + readabilityScore + " (Grade Level: " + 
            String.format("%.1f", gradeLevel) + ")");
        
        // Step 2: Check grammar using LanguageTool
        String fullText = title + " " + body;
        Map<String, Object> grammarResult = languageToolClient.checkGrammar(fullText, language);
        int grammarScore = (Integer) grammarResult.get("grammarScore");
        int errorCount = (Integer) grammarResult.get("errorCount");
        
        System.out.println("   ✏️  Grammar: " + grammarScore + " (Errors: " + errorCount + ")");
        
        // Step 3: Calculate structure score
        int structureScore = calculateStructureScore(title, body);
        
        System.out.println("   📐 Structure: " + structureScore);
        
        // Step 4: Calculate overall score (weighted average)
        int overallScore = (int) Math.round(
            (readabilityScore * 0.4) + 
            (grammarScore * 0.4) + 
            (structureScore * 0.2)
        );
        
        System.out.println("   ⭐ Overall Score: " + overallScore);
        
        // Build evaluation result
        Map<String, Object> evaluation = new HashMap<>();
        evaluation.put("overallScore", overallScore);
        evaluation.put("readabilityScore", readabilityScore);
        evaluation.put("fleschReadingEase", Math.round(fleschScore * 10.0) / 10.0);
        evaluation.put("fleschKincaidGradeLevel", Math.round(gradeLevel * 10.0) / 10.0);
        evaluation.put("grammarScore", grammarScore);
        evaluation.put("grammarErrorCount", errorCount);
        evaluation.put("structureScore", structureScore);
        
        // Add detailed feedback
        evaluation.put("feedback", generateFeedback(readabilityScore, grammarScore, structureScore, gradeLevel, errorCount, tone));
        
        // Add recommendations
        evaluation.put("recommendations", generateRecommendations(readabilityScore, grammarScore, structureScore, gradeLevel, errorCount));
        
        // Include grammar errors (if any)
        if (errorCount > 0) {
            evaluation.put("grammarErrors", grammarResult.get("errors"));
        }
        
        // Include original content for reference
        evaluation.put("evaluatedContent", content);
        
        return evaluation;
    }
    
    /**
     * Calculates structure score based on content organization.
     */
    private int calculateStructureScore(String title, String body) {
        int score = 0;
        
        // Title quality (0-30 points)
        if (title != null && !title.isEmpty()) {
            score += 20;
            if (title.length() >= 30 && title.length() <= 60) {
                score += 10; // Optimal title length
            }
        }
        
        // Body structure (0-70 points)
        if (body != null && !body.isEmpty()) {
            // Length check
            int wordCount = body.split("\\s+").length;
            if (wordCount >= 300) {
                score += 20; // Good length
            } else if (wordCount >= 200) {
                score += 10;
            }
            
            // Paragraph structure
            String[] paragraphs = body.split("\\n\\n+");
            if (paragraphs.length >= 3) {
                score += 20; // Good paragraph structure
            } else if (paragraphs.length >= 2) {
                score += 10;
            }
            
            // Sentence variety
            String[] sentences = body.split("[.!?]+");
            if (sentences.length >= 5) {
                score += 20; // Good sentence variety
            } else if (sentences.length >= 3) {
                score += 10;
            }
        }
        
        return Math.min(100, score);
    }
    
    /**
     * Generates feedback based on scores.
     */
    private String[] generateFeedback(int readabilityScore, int grammarScore, int structureScore, 
                                     double gradeLevel, int errorCount, String tone) {
        List<String> feedback = new ArrayList<>();
        
        // Readability feedback
        if (readabilityScore >= 80) {
            feedback.add("Excellent readability - content is easy to understand");
        } else if (readabilityScore >= 60) {
            feedback.add("Good readability - content is accessible to most readers");
        } else {
            feedback.add("Readability could be improved - consider simplifying sentence structure");
        }
        
        feedback.add("Reading level: Grade " + String.format("%.1f", gradeLevel));
        
        // Grammar feedback
        if (grammarScore >= 95) {
            feedback.add("Excellent grammar - no significant errors detected");
        } else if (grammarScore >= 85) {
            feedback.add("Good grammar - minor issues detected");
        } else if (errorCount > 0) {
            feedback.add("Grammar needs attention - " + errorCount + " error(s) found");
        }
        
        // Structure feedback
        if (structureScore >= 80) {
            feedback.add("Well-structured content with good organization");
        } else if (structureScore >= 60) {
            feedback.add("Content structure is adequate");
        } else {
            feedback.add("Content structure could be improved");
        }
        
        // Tone feedback
        if (tone != null && !tone.isEmpty()) {
            feedback.add("Tone: " + tone);
        }
        
        return feedback.toArray(new String[0]);
    }
    
    /**
     * Generates recommendations for improvement.
     */
    private String[] generateRecommendations(int readabilityScore, int grammarScore, int structureScore,
                                            double gradeLevel, int errorCount) {
        List<String> recommendations = new ArrayList<>();
        
        if (readabilityScore < 70) {
            recommendations.add("Simplify sentence structure to improve readability");
            if (gradeLevel > 12) {
                recommendations.add("Reduce average sentence length (currently Grade " + 
                    String.format("%.1f", gradeLevel) + " level)");
            }
        }
        
        if (grammarScore < 85) {
            recommendations.add("Review and fix grammar errors");
        }
        
        if (structureScore < 70) {
            recommendations.add("Add more paragraphs and improve content organization");
            recommendations.add("Consider adding headings and subheadings");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Content quality is excellent - no major improvements needed");
        }
        
        return recommendations.toArray(new String[0]);
    }
}