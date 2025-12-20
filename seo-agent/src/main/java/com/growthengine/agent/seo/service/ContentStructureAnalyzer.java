package com.growthengine.agent.seo.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ContentStructureAnalyzer {
    
    /**
     * Analyzes content structure for SEO.
     * 
     * @param content The content text
     * @return Structure analysis with scores and recommendations
     */
    public Map<String, Object> analyzeStructure(String content) {
        Map<String, Object> analysis = new HashMap<>();
        
        if (content == null || content.isEmpty()) {
            return createEmptyAnalysis();
        }
        
        // Count paragraphs
        String[] paragraphs = content.split("\\n\\n+");
        int paragraphCount = paragraphs.length;
        analysis.put("paragraphCount", paragraphCount);
        
        // Count sentences
        String[] sentences = content.split("[.!?]+");
        int sentenceCount = sentences.length;
        analysis.put("sentenceCount", sentenceCount);
        
        // Count words
        int wordCount = content.split("\\s+").length;
        analysis.put("wordCount", wordCount);
        
        // Average words per paragraph
        double avgWordsPerParagraph = paragraphCount > 0 ? (double) wordCount / paragraphCount : 0;
        analysis.put("avgWordsPerParagraph", Math.round(avgWordsPerParagraph * 10.0) / 10.0);
        
        // Average words per sentence
        double avgWordsPerSentence = sentenceCount > 0 ? (double) wordCount / sentenceCount : 0;
        analysis.put("avgWordsPerSentence", Math.round(avgWordsPerSentence * 10.0) / 10.0);
        
        // Check for headings (H1, H2, H3 patterns)
        int headingCount = countHeadings(content);
        analysis.put("headingCount", headingCount);
        
        // Structure score (0-100)
        int structureScore = calculateStructureScore(paragraphCount, sentenceCount, wordCount, headingCount);
        analysis.put("structureScore", structureScore);
        
        // Recommendations
        List<String> recommendations = generateStructureRecommendations(
            paragraphCount, sentenceCount, wordCount, headingCount, avgWordsPerSentence
        );
        analysis.put("recommendations", recommendations);
        
        return analysis;
    }
    
    private int countHeadings(String content) {
        // Look for common heading patterns
        int count = 0;
        String[] lines = content.split("\\n");
        
        for (String line : lines) {
            String trimmed = line.trim();
            // Check for markdown-style headings
            if (trimmed.startsWith("# ") || trimmed.startsWith("## ") || trimmed.startsWith("### ")) {
                count++;
            }
            // Check for all-caps lines (potential headings)
            else if (trimmed.length() > 5 && trimmed.length() < 100 && 
                     trimmed.equals(trimmed.toUpperCase()) && 
                     !trimmed.matches(".*[.!?]$")) {
                count++;
            }
        }
        
        return count;
    }
    
    private int calculateStructureScore(int paragraphs, int sentences, int words, int headings) {
        int score = 0;
        
        // Paragraph score (25 points)
        if (paragraphs >= 3 && paragraphs <= 10) {
            score += 25;
        } else if (paragraphs > 0) {
            score += 12;
        }
        
        // Sentence variety score (25 points)
        if (sentences >= 10) {
            score += 25;
        } else if (sentences >= 5) {
            score += 15;
        } else if (sentences > 0) {
            score += 5;
        }
        
        // Word count score (25 points)
        if (words >= 300) {
            score += 25;
        } else if (words >= 200) {
            score += 15;
        } else if (words >= 100) {
            score += 10;
        }
        
        // Heading score (25 points)
        if (headings >= 2) {
            score += 25;
        } else if (headings == 1) {
            score += 15;
        }
        
        return Math.min(100, score);
    }
    
    private List<String> generateStructureRecommendations(int paragraphs, int sentences, int words, 
                                                          int headings, double avgWordsPerSentence) {
        List<String> recommendations = new ArrayList<>();
        
        if (paragraphs < 3) {
            recommendations.add("Add more paragraphs to improve readability and structure");
        }
        
        if (headings == 0) {
            recommendations.add("Add headings (H2, H3) to improve content structure and SEO");
        } else if (headings < 2) {
            recommendations.add("Consider adding more headings to break up content");
        }
        
        if (words < 300) {
            recommendations.add("Content is too short. Aim for at least 300 words for better SEO");
        }
        
        if (avgWordsPerSentence > 25) {
            recommendations.add("Some sentences are too long. Consider breaking them up for better readability");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Content structure is well-optimized for SEO!");
        }
        
        return recommendations;
    }
    
    private Map<String, Object> createEmptyAnalysis() {
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("paragraphCount", 0);
        analysis.put("sentenceCount", 0);
        analysis.put("wordCount", 0);
        analysis.put("headingCount", 0);
        analysis.put("structureScore", 0);
        analysis.put("recommendations", Arrays.asList("Add content to analyze structure"));
        return analysis;
    }
}