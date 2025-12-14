package com.growthengine.agent.researcher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class DuckDuckGoClient {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private static final String DUCKDUCKGO_SEARCH_URL = "https://html.duckduckgo.com/html/";
    
    public DuckDuckGoClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
            .build();
    }
    
    /**
     * Searches DuckDuckGo for a topic and returns results.
     * 
     * @param topic The topic to search for
     * @return Map containing sources and additional information
     */
    public Map<String, Object> searchTopic(String topic) {
        try {
            System.out.println("🦆 Searching DuckDuckGo for: " + topic);
            
            // DuckDuckGo HTML search (no API key needed)
            String searchUrl = DUCKDUCKGO_SEARCH_URL + "?q=" + topic.replace(" ", "+");
            
            String htmlContent = webClient.get()
                .uri(searchUrl)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            if (htmlContent == null || htmlContent.isEmpty()) {
                return null;
            }
            
            // Parse HTML to extract results
            List<String> sources = extractSources(htmlContent);
            List<String> snippets = extractSnippets(htmlContent);
            
            // Build result
            Map<String, Object> result = new HashMap<>();
            result.put("sources", sources.toArray(new String[0]));
            result.put("snippets", snippets.toArray(new String[0]));
            result.put("sourceType", "DuckDuckGo");
            result.put("resultCount", sources.size());
            
            System.out.println("✅ DuckDuckGo search completed: " + sources.size() + " results");
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ Error searching DuckDuckGo: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extracts source URLs from DuckDuckGo HTML results.
     */
    private List<String> extractSources(String html) {
        List<String> sources = new ArrayList<>();
        
        try {
            Document doc = Jsoup.parse(html);
            Elements resultLinks = doc.select("a.result__url");
            
            for (Element link : resultLinks) {
                String url = link.attr("href");
                if (url != null && !url.isEmpty() && url.startsWith("http")) {
                    sources.add(url);
                    if (sources.size() >= 5) { // Limit to 5 sources
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing DuckDuckGo HTML: " + e.getMessage());
        }
        
        return sources;
    }
    
    /**
     * Extracts snippets from DuckDuckGo HTML results.
     */
    private List<String> extractSnippets(String html) {
        List<String> snippets = new ArrayList<>();
        
        try {
            Document doc = Jsoup.parse(html);
            Elements resultSnippets = doc.select("a.result__snippet");
            
            for (Element snippet : resultSnippets) {
                String text = snippet.text();
                if (text != null && !text.isEmpty() && text.length() > 20) {
                    snippets.add(text);
                    if (snippets.size() >= 3) { // Limit to 3 snippets
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting snippets: " + e.getMessage());
        }
        
        return snippets;
    }
}