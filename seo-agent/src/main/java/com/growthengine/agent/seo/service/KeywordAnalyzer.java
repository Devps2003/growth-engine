package com.growthengine.agent.seo.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KeywordAnalyzer {
    
    /**
     * Calculates keyword density for a specific keyword in content.
     * 
     * @param keyword The keyword to analyze
     * @param content The content text
     * @return Keyword density percentage (0-100)
     */
    public double calculateKeywordDensity(String keyword, String content) {
        if (keyword == null || content == null || keyword.isEmpty() || content.isEmpty()) {
            return 0.0;
        }
        
        // Normalize text (lowercase, remove punctuation)
        String normalizedContent = content.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", " ")
            .replaceAll("\\s+", " ");
        
        String normalizedKeyword = keyword.toLowerCase().trim();
        
        // Count total words
        String[] words = normalizedContent.split("\\s+");
        int totalWords = words.length;
        if (totalWords == 0) return 0.0;
        
        // Count keyword occurrences
        int keywordCount = 0;
        for (String word : words) {
            if (word.equals(normalizedKeyword)) {
                keywordCount++;
            }
        }
        
        // Calculate density (percentage)
        return (keywordCount * 100.0) / totalWords;
    }
    
    /**
     * Extracts keywords using TF-IDF-like approach (simplified).
     * Returns most important keywords based on frequency and relevance.
     * 
     * @param content The content text
     * @param topic The main topic
     * @param maxKeywords Maximum number of keywords to return
     * @return List of keywords sorted by importance
     */
    public List<String> extractKeywords(String content, String topic, int maxKeywords) {
        if (content == null || content.isEmpty()) {
            return Collections.singletonList(topic);
        }
        
        // Normalize content
        String normalized = content.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        
        // Common stop words to ignore
        Set<String> stopWords = new HashSet<>(Arrays.asList(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "as", "is", "was", "are", "were", "been",
            "be", "have", "has", "had", "do", "does", "did", "will", "would",
            "should", "could", "may", "might", "must", "can", "this", "that",
            "these", "those", "i", "you", "he", "she", "it", "we", "they",
            "what", "which", "who", "whom", "whose", "where", "when", "why", "how"
        ));
        
        // Count word frequencies
        Map<String, Integer> wordFreq = new HashMap<>();
        String[] words = normalized.split("\\s+");
        
        for (String word : words) {
            word = word.trim();
            // Skip stop words and very short words
            if (word.length() > 2 && !stopWords.contains(word)) {
                wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
            }
        }
        
        // Boost topic-related words
        String[] topicWords = topic.toLowerCase().split("\\s+");
        for (String topicWord : topicWords) {
            if (wordFreq.containsKey(topicWord)) {
                wordFreq.put(topicWord, wordFreq.get(topicWord) * 2); // Boost topic words
            }
        }
        
        // Sort by frequency and get top keywords
        List<String> keywords = wordFreq.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(maxKeywords)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // Always include topic as first keyword
        if (!keywords.contains(topic.toLowerCase())) {
            keywords.add(0, topic);
        }
        
        return keywords.stream().distinct().limit(maxKeywords).collect(Collectors.toList());
    }
    
    /**
     * Analyzes keyword distribution throughout content.
     * 
     * @param keyword The keyword to analyze
     * @param content The content text
     * @return Analysis result with distribution info
     */
    public Map<String, Object> analyzeKeywordDistribution(String keyword, String content) {
        Map<String, Object> analysis = new HashMap<>();
        
        double density = calculateKeywordDensity(keyword, content);
        analysis.put("density", Math.round(density * 10.0) / 10.0);
        analysis.put("densityStatus", getDensityStatus(density));
        
        // Count occurrences
        int occurrences = (int) Arrays.stream(content.toLowerCase().split("\\s+"))
            .filter(word -> word.equals(keyword.toLowerCase()))
            .count();
        analysis.put("occurrences", occurrences);
        
        // Check if keyword appears in first 100 words (important for SEO)
        String first100Words = content.length() > 500 ? content.substring(0, 500) : content;
        boolean inFirstParagraph = first100Words.toLowerCase().contains(keyword.toLowerCase());
        analysis.put("inFirstParagraph", inFirstParagraph);
        
        return analysis;
    }
    
    private String getDensityStatus(double density) {
        if (density < 0.5) {
            return "Too low - consider increasing keyword usage";
        } else if (density > 2.5) {
            return "Too high - risk of keyword stuffing";
        } else {
            return "Optimal - good keyword density";
        }
    }
}