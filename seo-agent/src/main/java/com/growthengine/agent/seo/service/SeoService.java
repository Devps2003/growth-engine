package com.growthengine.agent.seo.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SeoService {
    
    /**
     * Optimizes content for SEO by adding meta tags, keywords, and optimizing structure.
     * 
     * @param content The content to optimize (from Writer agent)
     * @param evaluationResult The evaluation result (from Evaluator agent) - optional
     * @param topic The topic of the content
     * @return SEO-optimized content with meta tags and keywords
     */
    public Map<String, Object> optimizeForSeo(
            Map<String, Object> content, 
            Map<String, Object> evaluationResult,
            String topic) {
        
        // TODO: Later, integrate with real SEO tools (Yoast, SEMrush APIs, etc.)
        // For now, return mock SEO optimization
        
        System.out.println("🔍 Optimizing content for SEO: " + topic);
        
        // Extract content details
        String title = (String) content.getOrDefault("title", "");
        String body = (String) content.getOrDefault("body", "");
        
        // Create SEO-optimized version
        Map<String, Object> seoOptimized = new HashMap<>();
        
        // 1. Optimize title (ensure it's SEO-friendly: 50-60 characters)
        String seoTitle = optimizeTitle(title, topic);
        seoOptimized.put("title", seoTitle);
        seoOptimized.put("originalTitle", title);
        
        // 2. Keep the body content
        seoOptimized.put("body", body);
        
        // 3. Generate meta description (150-160 characters)
        String metaDescription = generateMetaDescription(body, topic);
        seoOptimized.put("metaDescription", metaDescription);
        
        // 4. Extract and generate keywords
        List<String> keywords = extractKeywords(topic, body);
        seoOptimized.put("keywords", keywords);
        
        // 5. Generate Open Graph tags (for social media sharing)
        Map<String, String> openGraph = generateOpenGraphTags(seoTitle, metaDescription, topic);
        seoOptimized.put("openGraph", openGraph);
        
        // 6. Generate structured data (JSON-LD for rich snippets)
        Map<String, Object> structuredData = generateStructuredData(seoTitle, metaDescription, topic);
        seoOptimized.put("structuredData", structuredData);
        
        // 7. SEO recommendations
        List<String> recommendations = generateSeoRecommendations(title, body, topic);
        seoOptimized.put("seoRecommendations", recommendations);
        
        // 8. Calculate SEO score
        int seoScore = calculateSeoScore(seoTitle, metaDescription, keywords, body);
        seoOptimized.put("seoScore", seoScore);
        
        // 9. Keep original content and evaluation for reference
        seoOptimized.put("originalContent", content);
        if (evaluationResult != null) {
            seoOptimized.put("evaluationResult", evaluationResult);
        }
        
        // 10. Add topic and other metadata
        seoOptimized.put("topic", topic);
        seoOptimized.put("tone", content.getOrDefault("tone", "professional"));
        seoOptimized.put("language", content.getOrDefault("language", "English"));
        
        return seoOptimized;
    }
    
    /**
     * Optimizes title for SEO (50-60 characters, includes keywords).
     */
    private String optimizeTitle(String originalTitle, String topic) {
        if (originalTitle == null || originalTitle.isEmpty()) {
            return "Complete Guide to " + topic;
        }
        
        // Ensure title is between 50-60 characters (optimal for SEO)
        if (originalTitle.length() > 60) {
            // Truncate and add ellipsis
            return originalTitle.substring(0, 57) + "...";
        } else if (originalTitle.length() < 30) {
            // Add topic if title is too short
            return originalTitle + " - " + topic;
        }
        
        return originalTitle;
    }
    
    /**
     * Generates meta description from content (150-160 characters).
     */
    private String generateMetaDescription(String body, String topic) {
        if (body == null || body.isEmpty()) {
            return "Learn everything about " + topic + ". Comprehensive guide with expert insights and practical tips.";
        }
        
        // Take first 155 characters of body, ensure it ends with a sentence
        String description = body.length() > 155 ? body.substring(0, 155) : body;
        
        // Try to end at a sentence boundary
        int lastPeriod = description.lastIndexOf('.');
        if (lastPeriod > 100) {
            description = description.substring(0, lastPeriod + 1);
        } else {
            description = description.trim() + "...";
        }
        
        // Ensure it's between 150-160 characters
        if (description.length() < 150) {
            description += " Learn more about " + topic + ".";
        }
        
        return description.length() > 160 ? description.substring(0, 157) + "..." : description;
    }
    
    /**
     * Extracts keywords from topic and content.
     */
    private List<String> extractKeywords(String topic, String body) {
        List<String> keywords = new ArrayList<>();
        
        // Add topic as primary keyword
        keywords.add(topic.toLowerCase());
        
        // Extract words from topic
        String[] topicWords = topic.toLowerCase().split("\\s+");
        keywords.addAll(Arrays.asList(topicWords));
        
        // Add common SEO keywords based on topic
        keywords.add("guide");
        keywords.add("tips");
        keywords.add("best practices");
        
        // Remove duplicates and limit to 10 keywords
        Set<String> uniqueKeywords = new LinkedHashSet<>(keywords);
        return new ArrayList<>(uniqueKeywords).subList(0, Math.min(10, uniqueKeywords.size()));
    }
    
    /**
     * Generates Open Graph tags for social media sharing.
     */
    private Map<String, String> generateOpenGraphTags(String title, String description, String topic) {
        Map<String, String> ogTags = new HashMap<>();
        ogTags.put("og:title", title);
        ogTags.put("og:description", description);
        ogTags.put("og:type", "article");
        ogTags.put("og:site_name", "Growth Engine");
        // In real implementation, you'd add og:image, og:url, etc.
        return ogTags;
    }
    
    /**
     * Generates structured data (JSON-LD) for rich snippets in search results.
     */
    private Map<String, Object> generateStructuredData(String title, String description, String topic) {
        Map<String, Object> structuredData = new HashMap<>();
        structuredData.put("@context", "https://schema.org");
        structuredData.put("@type", "Article");
        structuredData.put("headline", title);
        structuredData.put("description", description);
        structuredData.put("about", Map.of("@type", "Thing", "name", topic));
        // In real implementation, add author, datePublished, etc.
        return structuredData;
    }
    
    /**
     * Generates SEO recommendations.
     */
    private List<String> generateSeoRecommendations(String title, String body, String topic) {
        List<String> recommendations = new ArrayList<>();
        
        if (title == null || title.length() < 30) {
            recommendations.add("Title is too short. Aim for 50-60 characters.");
        }
        
        if (body == null || body.length() < 300) {
            recommendations.add("Content is too short. Aim for at least 300 words for better SEO.");
        }
        
        if (body != null && !body.toLowerCase().contains(topic.toLowerCase())) {
            recommendations.add("Ensure the topic keyword appears naturally in the content.");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Content is well-optimized for SEO!");
        }
        
        return recommendations;
    }
    
    /**
     * Calculates SEO score (0-100).
     */
    private int calculateSeoScore(String title, String metaDescription, List<String> keywords, String body) {
        int score = 0;
        
        // Title score (30 points)
        if (title != null && title.length() >= 30 && title.length() <= 60) {
            score += 30;
        } else if (title != null && title.length() > 0) {
            score += 15;
        }
        
        // Meta description score (25 points)
        if (metaDescription != null && metaDescription.length() >= 150 && metaDescription.length() <= 160) {
            score += 25;
        } else if (metaDescription != null && metaDescription.length() > 0) {
            score += 12;
        }
        
        // Keywords score (20 points)
        if (keywords != null && keywords.size() >= 5) {
            score += 20;
        } else if (keywords != null && keywords.size() > 0) {
            score += 10;
        }
        
        // Content length score (25 points)
        if (body != null && body.length() >= 300) {
            score += 25;
        } else if (body != null && body.length() > 0) {
            score += 12;
        }
        
        return Math.min(100, score);
    }
}