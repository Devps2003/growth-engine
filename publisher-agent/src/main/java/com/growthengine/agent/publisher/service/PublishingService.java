package com.growthengine.agent.publisher.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class PublishingService {
    
    @Autowired
    private WordPressClient wordPressClient;
    
    /**
     * Publishes content to a CMS/blog platform.
     * Tries WordPress first, falls back to mock if not configured.
     * 
     * @param seoOptimizedContent The SEO-optimized content from SEO agent
     * @param topic The topic of the content
     * @return Publishing result with URL and status
     */
    public Map<String, Object> publishContent(Map<String, Object> seoOptimizedContent, String topic) {
        System.out.println("📰 Publishing content: " + topic);
        
        // Extract content details
        String title = (String) seoOptimizedContent.getOrDefault("title", "");
        String body = (String) seoOptimizedContent.getOrDefault("body", "");
        String metaDescription = (String) seoOptimizedContent.getOrDefault("metaDescription", "");
        
        // Generate slug
        String slug = generateSlug(title, topic);
        
        // Try WordPress first (if configured)
        if (wordPressClient.isEnabled()) {
            Map<String, Object> wordPressResult = wordPressClient.publishPost(
                title,
                body,
                metaDescription,
                slug,
                (List<String>) seoOptimizedContent.getOrDefault("keywords", new ArrayList<>()),
                seoOptimizedContent
            );
            
            if (wordPressResult != null) {
                // WordPress publishing succeeded
                Map<String, Object> publishingResult = new HashMap<>(wordPressResult);
                publishingResult.put("publishedAt", new Date().toString());
                publishingResult.put("slug", slug);
                publishingResult.put("publishedContent", Map.of(
                    "title", title,
                    "body", body,
                    "metaDescription", metaDescription
                ));
                
                // Include SEO metadata
                addSeoMetadata(publishingResult, seoOptimizedContent);
                publishingResult.put("topic", topic);
                publishingResult.put("publishingPlatform", "WordPress");
                
                System.out.println("✅ Content published successfully to WordPress: " + 
                    publishingResult.get("publishedUrl"));
                
                return publishingResult;
            }
        }
        
        // Fallback to mock publishing (if WordPress not configured or failed)
        System.out.println("⚠️ Using mock publishing (WordPress not configured or failed)");
        return createMockPublishingResult(title, body, metaDescription, slug, seoOptimizedContent, topic);
    }
    
    /**
     * Creates mock publishing result (fallback).
     */
    private Map<String, Object> createMockPublishingResult(String title, String body, String metaDescription,
                                                          String slug, Map<String, Object> seoOptimizedContent,
                                                          String topic) {
        String publishedUrl = "https://example.com/articles/" + slug;
        
        Map<String, Object> publishingResult = new HashMap<>();
        publishingResult.put("published", true);
        publishingResult.put("publishedUrl", publishedUrl);
        publishingResult.put("publishedAt", new Date().toString());
        publishingResult.put("slug", slug);
        publishingResult.put("publishingPlatform", "Mock (WordPress not configured)");
        
        // Include published content
        publishingResult.put("publishedContent", Map.of(
            "title", title,
            "body", body,
            "metaDescription", metaDescription
        ));
        
        // Include SEO metadata
        addSeoMetadata(publishingResult, seoOptimizedContent);
        publishingResult.put("topic", topic);
        
        System.out.println("✅ Mock publishing completed: " + publishedUrl);
        
        return publishingResult;
    }
    
    /**
     * Adds SEO metadata to publishing result.
     */
    private void addSeoMetadata(Map<String, Object> result, Map<String, Object> seoOptimizedContent) {
        if (seoOptimizedContent.containsKey("keywords")) {
            result.put("keywords", seoOptimizedContent.get("keywords"));
        }
        if (seoOptimizedContent.containsKey("openGraph")) {
            result.put("openGraph", seoOptimizedContent.get("openGraph"));
        }
        if (seoOptimizedContent.containsKey("structuredData")) {
            result.put("structuredData", seoOptimizedContent.get("structuredData"));
        }
        if (seoOptimizedContent.containsKey("seoScore")) {
            result.put("seoScore", seoOptimizedContent.get("seoScore"));
        }
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