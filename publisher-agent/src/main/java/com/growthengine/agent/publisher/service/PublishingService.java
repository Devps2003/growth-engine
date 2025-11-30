package com.growthengine.agent.publisher.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class PublishingService {
    
    /**
     * Publishes content to a CMS/blog platform.
     * 
     * @param seoOptimizedContent The SEO-optimized content from SEO agent
     * @param topic The topic of the content
     * @return Publishing result with URL and status
     */
    public Map<String, Object> publishContent(Map<String, Object> seoOptimizedContent, String topic) {
        // TODO: Later, integrate with real CMS APIs (WordPress, Contentful, etc.)
        // For now, return mock publishing result
        
        System.out.println("📰 Publishing content: " + topic);
        
        // Extract content details
        String title = (String) seoOptimizedContent.getOrDefault("title", "");
        String body = (String) seoOptimizedContent.getOrDefault("body", "");
        String metaDescription = (String) seoOptimizedContent.getOrDefault("metaDescription", "");
        
        // Generate mock published URL
        String slug = generateSlug(title, topic);
        String publishedUrl = "https://example.com/articles/" + slug;
        
        // Build publishing result
        Map<String, Object> publishingResult = new HashMap<>();
        publishingResult.put("published", true);
        publishingResult.put("publishedUrl", publishedUrl);
        publishingResult.put("publishedAt", new Date().toString());
        publishingResult.put("slug", slug);
        
        // Include published content
        publishingResult.put("publishedContent", Map.of(
            "title", title,
            "body", body,
            "metaDescription", metaDescription
        ));
        
        // Include SEO metadata
        if (seoOptimizedContent.containsKey("keywords")) {
            publishingResult.put("keywords", seoOptimizedContent.get("keywords"));
        }
        if (seoOptimizedContent.containsKey("openGraph")) {
            publishingResult.put("openGraph", seoOptimizedContent.get("openGraph"));
        }
        if (seoOptimizedContent.containsKey("structuredData")) {
            publishingResult.put("structuredData", seoOptimizedContent.get("structuredData"));
        }
        
        // Include SEO score
        if (seoOptimizedContent.containsKey("seoScore")) {
            publishingResult.put("seoScore", seoOptimizedContent.get("seoScore"));
        }
        
        // Add topic
        publishingResult.put("topic", topic);
        
        System.out.println("✅ Content published successfully: " + publishedUrl);
        
        return publishingResult;
    }
    
    /**
     * Generates a URL-friendly slug from title and topic.
     */
    private String generateSlug(String title, String topic) {
        // Use topic if title is empty
        String base = title != null && !title.isEmpty() ? title : topic;
        
        // Convert to lowercase, replace spaces with hyphens, remove special chars
        String slug = base.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters
            .replaceAll("\\s+", "-") // Replace spaces with hyphens
            .replaceAll("-+", "-") // Replace multiple hyphens with single
            .trim();
        
        // Add timestamp for uniqueness
        long timestamp = System.currentTimeMillis() / 1000;
        return slug + "-" + timestamp;
    }
}