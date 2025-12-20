package com.growthengine.agent.publisher.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import jakarta.annotation.PostConstruct;

import java.util.*;

@Service
public class WordPressClient {
    
    @Value("${wordpress.api.url:}")
    private String wordpressUrl;
    
    @Value("${wordpress.api.username:}")
    private String username;
    
    @Value("${wordpress.api.password:}")
    private String password;
    
    @Value("${wordpress.api.enabled:false}")
    private boolean enabled;
    
    private WebClient webClient;
    
    @PostConstruct
    public void init() {
        if (wordpressUrl != null && !wordpressUrl.isEmpty()) {
            this.webClient = WebClient.builder()
                .baseUrl(wordpressUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        }
    }
    
    /**
     * Publishes a post to WordPress.
     * 
     * @param title Post title
     * @param content Post content (body)
     * @param excerpt Post excerpt (meta description)
     * @param slug URL slug
     * @param keywords List of keywords for tags
     * @param seoMeta SEO metadata (meta description, etc.)
     * @return Published post data with URL
     */
    public Map<String, Object> publishPost(String title, String content, String excerpt, 
                                           String slug, List<String> keywords, 
                                           Map<String, Object> seoMeta) {
        if (!enabled || wordpressUrl == null || wordpressUrl.isEmpty()) {
            System.out.println("⚠️ WordPress integration not configured, using mock publishing");
            return null;
        }
        
        try {
            System.out.println("📝 Publishing to WordPress: " + wordpressUrl);
            
            // Build WordPress post payload
            Map<String, Object> postData = new HashMap<>();
            postData.put("title", title);
            postData.put("content", content);
            postData.put("excerpt", excerpt);
            postData.put("slug", slug);
            postData.put("status", "publish"); // or "draft" for testing
            
            // Add tags from keywords
            if (keywords != null && !keywords.isEmpty()) {
                postData.put("tags", keywords);
            }
            
            // Add SEO meta as custom fields
            Map<String, Object> meta = new HashMap<>();
            if (seoMeta != null) {
                if (seoMeta.containsKey("metaDescription")) {
                    meta.put("_yoast_wpseo_metadesc", seoMeta.get("metaDescription"));
                }
                if (seoMeta.containsKey("keywords")) {
                    meta.put("_yoast_wpseo_focuskw", keywords != null && !keywords.isEmpty() ? keywords.get(0) : "");
                }
            }
            if (!meta.isEmpty()) {
                postData.put("meta", meta);
            }
            
            // Create Basic Auth header (WordPress REST API uses Application Password)
            String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
            
            // Make API call
            Map<String, Object> response = webClient.post()
                .uri("/wp-json/wp/v2/posts")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                .bodyValue(postData)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            if (response != null && response.containsKey("link")) {
                String publishedUrl = (String) response.get("link");
                Long postId = response.get("id") instanceof Number ? 
                    ((Number) response.get("id")).longValue() : null;
                
                Map<String, Object> result = new HashMap<>();
                result.put("published", true);
                result.put("publishedUrl", publishedUrl);
                result.put("postId", postId);
                result.put("wordpressUrl", wordpressUrl);
                
                System.out.println("✅ Published to WordPress: " + publishedUrl);
                return result;
            }
            
            return null;
            
        } catch (Exception e) {
            System.err.println("❌ Error publishing to WordPress: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Checks if WordPress integration is enabled and configured.
     */
    public boolean isEnabled() {
        return enabled && wordpressUrl != null && !wordpressUrl.isEmpty() && 
               username != null && !username.isEmpty() && 
               password != null && !password.isEmpty();
    }
}

