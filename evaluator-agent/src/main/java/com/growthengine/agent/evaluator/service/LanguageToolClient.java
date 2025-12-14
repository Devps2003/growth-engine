package com.growthengine.agent.evaluator.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class LanguageToolClient {
    
    private final WebClient webClient;
    private static final String LANGUAGETOOL_API_URL = "https://api.languagetool.org/v2";
    
    public LanguageToolClient() {
        this.webClient = WebClient.builder()
            .baseUrl(LANGUAGETOOL_API_URL)
            .build();
    }
    
    /**
     * Checks grammar and spelling using LanguageTool API.
     * 
     * @param text The text to check
     * @param language Language code (e.g., "en-US", "en-GB")
     * @return Grammar check result with errors and score
     */
    public Map<String, Object> checkGrammar(String text, String language) {
        try {
            if (text == null || text.trim().isEmpty()) {
                return createEmptyResult();
            }
            
            // Limit text length (LanguageTool has limits)
            String textToCheck = text.length() > 50000 ? text.substring(0, 50000) : text;
            
            // Make API call (LanguageTool uses form-urlencoded, not JSON)
            String requestBody = "text=" + URLEncoder.encode(textToCheck, StandardCharsets.UTF_8) + 
                                "&language=" + (language != null ? language : "en-US");
            
            Map<String, Object> response = webClient.post()
                .uri("/check")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            if (response == null) {
                return createEmptyResult();
            }
            
            // Extract matches (errors)
            List<Map<String, Object>> matches = (List<Map<String, Object>>) response.get("matches");
            int errorCount = matches != null ? matches.size() : 0;
            
            // Calculate grammar score (100 - errors per 100 words)
            int wordCount = text.split("\\s+").length;
            double errorsPer100Words = wordCount > 0 ? (errorCount * 100.0 / wordCount) : 0;
            int grammarScore = Math.max(0, (int) (100 - errorsPer100Words));
            
            // Build result
            Map<String, Object> result = new HashMap<>();
            result.put("grammarScore", grammarScore);
            result.put("errorCount", errorCount);
            result.put("wordCount", wordCount);
            result.put("errorsPer100Words", Math.round(errorsPer100Words * 10.0) / 10.0);
            
            // Extract error details
            List<Map<String, Object>> errorDetails = new ArrayList<>();
            if (matches != null) {
                for (Map<String, Object> match : matches) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("message", match.get("message"));
                    error.put("shortMessage", match.get("shortMessage"));
                    error.put("offset", match.get("offset"));
                    error.put("length", match.get("length"));
                    
                    List<Map<String, Object>> replacements = (List<Map<String, Object>>) match.get("replacements");
                    if (replacements != null && !replacements.isEmpty()) {
                        error.put("suggestion", replacements.get(0).get("value"));
                    }
                    
                    errorDetails.add(error);
                }
            }
            result.put("errors", errorDetails);
            
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ Error calling LanguageTool API: " + e.getMessage());
            // Return fallback result
            return createEmptyResult();
        }
    }
    
    private Map<String, Object> createEmptyResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("grammarScore", 85); // Default score
        result.put("errorCount", 0);
        result.put("wordCount", 0);
        result.put("errorsPer100Words", 0.0);
        result.put("errors", new ArrayList<>());
        return result;
    }
}