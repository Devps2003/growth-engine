package com.growthengine.agent.researcher.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ResearchService {
    
    @Autowired
    private WikipediaClient wikipediaClient;
    
    @Autowired
    private DuckDuckGoClient duckDuckGoClient;
    
    /**
     * Performs research on a topic using Wikipedia and DuckDuckGo.
     * 
     * @param topic The topic to research
     * @return Research result with summary, key points, and sources
     */
    public Map<String, Object> performResearch(String topic) {
        System.out.println("🔍 Performing research on topic: " + topic);
        
        Map<String, Object> researchResult = new HashMap<>();
        researchResult.put("topic", topic);
        
        // Step 1: Get Wikipedia data (primary source - structured, reliable)
        Map<String, Object> wikipediaData = wikipediaClient.searchTopic(topic);
        
        // Step 2: Get DuckDuckGo data (secondary source - recent, diverse)
        Map<String, Object> duckDuckGoData = duckDuckGoClient.searchTopic(topic);
        
        // Step 3: Combine results
        combineResearchResults(researchResult, wikipediaData, duckDuckGoData);
        
        // Step 4: Ensure we have at least basic data
        if (researchResult.get("summary") == null || 
            ((String) researchResult.get("summary")).isEmpty()) {
            // Fallback if both APIs fail
            researchResult.put("summary", "Research data for: " + topic);
            researchResult.put("keyPoints", new String[]{
                "Key information about " + topic,
                "Important aspects of " + topic
            });
            researchResult.put("sources", new String[]{"Research sources"});
            System.out.println("⚠️ Using fallback research data");
        }
        
        System.out.println("✅ Research completed for: " + topic);
        return researchResult;
    }
    
    /**
     * Combines Wikipedia and DuckDuckGo research results.
     */
    private void combineResearchResults(Map<String, Object> result, 
                                       Map<String, Object> wikipediaData,
                                       Map<String, Object> duckDuckGoData) {
        
        // Combine summaries
        StringBuilder summary = new StringBuilder();
        
        if (wikipediaData != null && wikipediaData.containsKey("summary")) {
            summary.append((String) wikipediaData.get("summary"));
        }
        
        if (duckDuckGoData != null && duckDuckGoData.containsKey("snippets")) {
            String[] snippets = (String[]) duckDuckGoData.get("snippets");
            if (snippets != null && snippets.length > 0) {
                if (summary.length() > 0) {
                    summary.append(" ");
                }
                summary.append(snippets[0]); // Add first snippet
            }
        }
        
        result.put("summary", summary.toString().trim());
        
        // Combine key points
        List<String> allKeyPoints = new ArrayList<>();
        
        if (wikipediaData != null && wikipediaData.containsKey("keyPoints")) {
            String[] wikiPoints = (String[]) wikipediaData.get("keyPoints");
            if (wikiPoints != null) {
                allKeyPoints.addAll(Arrays.asList(wikiPoints));
            }
        }
        
        if (duckDuckGoData != null && duckDuckGoData.containsKey("snippets")) {
            String[] snippets = (String[]) duckDuckGoData.get("snippets");
            if (snippets != null) {
                // Add snippets as additional key points (limit to 2)
                int count = Math.min(2, snippets.length);
                for (int i = 0; i < count; i++) {
                    allKeyPoints.add(snippets[i]);
                }
            }
        }
        
        // Limit to 5 key points total
        if (allKeyPoints.size() > 5) {
            allKeyPoints = allKeyPoints.subList(0, 5);
        }
        
        result.put("keyPoints", allKeyPoints.toArray(new String[0]));
        
        // Combine sources
        List<String> allSources = new ArrayList<>();
        
        if (wikipediaData != null && wikipediaData.containsKey("sources")) {
            String[] wikiSources = (String[]) wikipediaData.get("sources");
            if (wikiSources != null) {
                allSources.addAll(Arrays.asList(wikiSources));
            }
        }
        
        if (duckDuckGoData != null && duckDuckGoData.containsKey("sources")) {
            String[] ddgSources = (String[]) duckDuckGoData.get("sources");
            if (ddgSources != null) {
                allSources.addAll(Arrays.asList(ddgSources));
            }
        }
        
        // Remove duplicates and limit to 10 sources
        Set<String> uniqueSources = new LinkedHashSet<>(allSources);
        List<String> finalSources = new ArrayList<>(uniqueSources);
        if (finalSources.size() > 10) {
            finalSources = finalSources.subList(0, 10);
        }
        
        result.put("sources", finalSources.toArray(new String[0]));
        
        // Add metadata
        result.put("researchMethod", "Wikipedia + DuckDuckGo");
        result.put("sourceCount", finalSources.size());
    }
}