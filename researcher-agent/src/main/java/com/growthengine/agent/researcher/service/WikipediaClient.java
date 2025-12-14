package com.growthengine.agent.researcher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class WikipediaClient {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private static final String WIKIPEDIA_API_URL = "https://en.wikipedia.org/api/rest_v1";
    
    public WikipediaClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
            .baseUrl(WIKIPEDIA_API_URL)
            .build();
    }
    
    /**
     * Searches Wikipedia for a topic and returns summary.
     * 
     * @param topic The topic to search for
     * @return Map containing summary, key points, and sources
     */
    public Map<String, Object> searchTopic(String topic) {
        try {
            System.out.println("📚 Searching Wikipedia for: " + topic);
            
            // Step 1: Search for the topic (get page title)
            String pageTitle = searchPageTitle(topic);
            
            if (pageTitle == null) {
                System.out.println("⚠️ No Wikipedia page found for: " + topic);
                return null;
            }
            
            // Step 2: Get page summary
            String summary = getPageSummary(pageTitle);
            
            // Step 3: Get key points (first few sections)
            List<String> keyPoints = getKeyPoints(pageTitle);
            
            // Step 4: Build Wikipedia URL
            String wikipediaUrl = "https://en.wikipedia.org/wiki/" + pageTitle.replace(" ", "_");
            
            // Build result
            Map<String, Object> result = new HashMap<>();
            result.put("summary", summary != null ? summary : "No summary available");
            result.put("keyPoints", keyPoints.toArray(new String[0]));
            result.put("sources", new String[]{wikipediaUrl});
            result.put("sourceType", "Wikipedia");
            
            System.out.println("✅ Wikipedia research completed for: " + topic);
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ Error searching Wikipedia: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Searches Wikipedia to find the page title for a topic.
     */
    private String searchPageTitle(String topic) {
        try {
            // Use Wikipedia search API
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/page/summary/{title}")
                    .build(topic.replace(" ", "_")))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            if (response != null && response.containsKey("title")) {
                return (String) response.get("title");
            }
            
            // If direct search fails, try search endpoint
            return searchForPageTitle(topic);
            
        } catch (Exception e) {
            // Try alternative search
            return searchForPageTitle(topic);
        }
    }
    
    /**
     * Alternative search method using Wikipedia search API.
     */
    private String searchForPageTitle(String topic) {
        try {
            // Use Wikipedia search API (different base URL)
            WebClient searchClient = WebClient.builder()
                .baseUrl("https://en.wikipedia.org")
                .build();
            
            String searchPath = "/w/api.php?action=query&list=search&srsearch=" 
                + topic.replace(" ", "%20") 
                + "&format=json&srlimit=1";
            
            Map<String, Object> response = searchClient.get()
                .uri(searchPath)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            if (response != null) {
                Map<String, Object> query = (Map<String, Object>) response.get("query");
                if (query != null) {
                    List<Map<String, Object>> search = (List<Map<String, Object>>) query.get("search");
                    if (search != null && !search.isEmpty()) {
                        return (String) search.get(0).get("title");
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("Error in Wikipedia search: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets the summary of a Wikipedia page.
     */
    private String getPageSummary(String pageTitle) {
        try {
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/page/summary/{title}")
                    .build(pageTitle.replace(" ", "_")))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            if (response != null && response.containsKey("extract")) {
                String extract = (String) response.get("extract");
                // Limit to first 500 characters for summary
                if (extract.length() > 500) {
                    return extract.substring(0, 500) + "...";
                }
                return extract;
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Extracts key points from Wikipedia page (first few sections).
     */
    private List<String> getKeyPoints(String pageTitle) {
        List<String> keyPoints = new ArrayList<>();
        
        try {
            // Get page content
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/page/summary/{title}")
                    .build(pageTitle.replace(" ", "_")))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            if (response != null) {
                String extract = (String) response.get("extract");
                if (extract != null) {
                    // Extract first few sentences as key points
                    String[] sentences = extract.split("\\. ");
                    int count = Math.min(3, sentences.length);
                    for (int i = 0; i < count; i++) {
                        String sentence = sentences[i].trim();
                        if (!sentence.isEmpty() && sentence.length() > 20) {
                            keyPoints.add(sentence + ".");
                        }
                    }
                }
            }
        } catch (Exception e) {
            // If detailed extraction fails, use summary
            String summary = getPageSummary(pageTitle);
            if (summary != null) {
                keyPoints.add(summary);
            }
        }
        
        // Ensure we have at least some key points
        if (keyPoints.isEmpty()) {
            keyPoints.add("Information about " + pageTitle);
        }
        
        return keyPoints;
    }
}