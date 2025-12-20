package com.growthengine.agent.seo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SeoService {

    @Autowired
    private KeywordAnalyzer keywordAnalyzer;

    @Autowired
    private ContentStructureAnalyzer contentStructureAnalyzer;
    
    /**
     * Optimizes content for SEO by adding meta tags, keywords, and optimizing structure.
     * Uses real SEO algorithms for keyword analysis and content structure.
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
        
        // 4. Extract keywords using real TF-IDF algorithm
        List<String> keywords = keywordAnalyzer.extractKeywords(body, topic, 10);
        seoOptimized.put("keywords", keywords);
        
        // 5. Add keyword analysis (density, distribution)
        if (!keywords.isEmpty() && body != null && !body.isEmpty()) {
            String primaryKeyword = keywords.get(0);
            Map<String, Object> keywordAnalysis = keywordAnalyzer.analyzeKeywordDistribution(primaryKeyword, body);
            seoOptimized.put("keywordAnalysis", keywordAnalysis);
            System.out.println("   🔑 Primary keyword: " + primaryKeyword + 
                " (Density: " + keywordAnalysis.get("density") + "%)");
        }
        
        // 6. Generate Open Graph tags (for social media sharing)
        Map<String, String> openGraph = generateOpenGraphTags(seoTitle, metaDescription, topic);
        seoOptimized.put("openGraph", openGraph);
        
        // 7. Generate structured data (JSON-LD for rich snippets)
        Map<String, Object> structuredData = generateStructuredData(seoTitle, metaDescription, topic);
        seoOptimized.put("structuredData", structuredData);
        
        // 8. Analyze content structure
        Map<String, Object> structureAnalysis = contentStructureAnalyzer.analyzeStructure(body);
        seoOptimized.put("structureAnalysis", structureAnalysis);
        System.out.println("   📐 Structure Score: " + structureAnalysis.get("structureScore") + 
            " (Paragraphs: " + structureAnalysis.get("paragraphCount") + 
            ", Headings: " + structureAnalysis.get("headingCount") + ")");
        
        // 9. Generate SEO recommendations based on real analysis
        List<String> recommendations = generateSeoRecommendations(title, body, topic);
        seoOptimized.put("seoRecommendations", recommendations);
        
        // 10. Calculate advanced SEO score using multiple factors
        int seoScore = calculateAdvancedSeoScore(seoTitle, metaDescription, keywords, body, evaluationResult);
        seoOptimized.put("seoScore", seoScore);
        System.out.println("   ⭐ SEO Score: " + seoScore + "/100");
        
        // 11. Keep original content and evaluation for reference
        seoOptimized.put("originalContent", content);
        if (evaluationResult != null) {
            seoOptimized.put("evaluationResult", evaluationResult);
        }
        
        // 12. Add topic and other metadata
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
     * Generates SEO recommendations based on real analysis.
     */
    private List<String> generateSeoRecommendations(String title, String body, String topic) {
        List<String> recommendations = new ArrayList<>();
        
        // Title recommendations
        if (title == null || title.length() < 30) {
            recommendations.add("Title is too short. Aim for 50-60 characters for optimal SEO.");
        } else if (title.length() > 60) {
            recommendations.add("Title is too long. Keep it under 60 characters to avoid truncation in search results.");
        }
        
        // Keyword recommendations
        if (body != null && !body.toLowerCase().contains(topic.toLowerCase())) {
            recommendations.add("Ensure the primary topic keyword appears naturally in the content body.");
        }
        
        // Keyword density recommendations
        if (body != null && !body.isEmpty() && topic != null && !topic.isEmpty()) {
            double density = keywordAnalyzer.calculateKeywordDensity(topic, body);
            if (density < 0.5) {
                recommendations.add("Keyword density is low (" + String.format("%.1f", density) + 
                    "%). Consider using the topic keyword more naturally throughout the content.");
            } else if (density > 2.5) {
                recommendations.add("Keyword density is too high (" + String.format("%.1f", density) + 
                    "%). Avoid keyword stuffing - use synonyms and related terms.");
            }
        }
        
        // Structure recommendations from analyzer
        if (body != null && !body.isEmpty()) {
            Map<String, Object> structureAnalysis = contentStructureAnalyzer.analyzeStructure(body);
            List<String> structureRecs = (List<String>) structureAnalysis.get("recommendations");
            recommendations.addAll(structureRecs);
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Content is well-optimized for SEO!");
        }
        
        return recommendations;
    }
    
    /**
     * Calculates advanced SEO score using multiple factors and real algorithms.
     * 
     * @param title The optimized title
     * @param metaDescription The meta description
     * @param keywords List of extracted keywords
     * @param body The content body
     * @param evaluationResult Optional evaluation result from Evaluator agent
     * @return SEO score (0-100)
     */
    private int calculateAdvancedSeoScore(String title, String metaDescription, List<String> keywords, 
                                        String body, Map<String, Object> evaluationResult) {
        int score = 0;
        
        // Title score (20 points)
        if (title != null && title.length() >= 30 && title.length() <= 60) {
            score += 20;
        } else if (title != null && title.length() > 0) {
            score += 10;
        }
        
        // Meta description score (20 points)
        if (metaDescription != null && metaDescription.length() >= 150 && metaDescription.length() <= 160) {
            score += 20;
        } else if (metaDescription != null && metaDescription.length() > 0) {
            score += 10;
        }
        
        // Keywords score (15 points)
        if (keywords != null && keywords.size() >= 5) {
            score += 15;
        } else if (keywords != null && keywords.size() > 0) {
            score += 8;
        }
        
        // Keyword density score (15 points)
        if (!keywords.isEmpty() && body != null && !body.isEmpty()) {
            String primaryKeyword = keywords.get(0);
            double density = keywordAnalyzer.calculateKeywordDensity(primaryKeyword, body);
            if (density >= 0.5 && density <= 2.5) {
                score += 15; // Optimal density
            } else if (density > 0 && density < 0.5) {
                score += 7; // Present but too low
            } else if (density > 2.5 && density <= 4.0) {
                score += 5; // Too high but not extreme
            }
            // density > 4.0 gets 0 points (keyword stuffing)
        }
        
        // Content length score (15 points)
        if (body != null && body.length() >= 300) {
            score += 15;
        } else if (body != null && body.length() >= 200) {
            score += 10;
        } else if (body != null && body.length() >= 100) {
            score += 5;
        }
        
        // Structure score (15 points) - from structure analyzer
        if (body != null && !body.isEmpty()) {
            Map<String, Object> structureAnalysis = contentStructureAnalyzer.analyzeStructure(body);
            int structureScore = (Integer) structureAnalysis.get("structureScore");
            score += (int) (structureScore * 0.15); // Convert to 15-point scale
        }
        
        // Readability bonus (if evaluation result available)
        if (evaluationResult != null && evaluationResult.containsKey("readabilityScore")) {
            Object readabilityObj = evaluationResult.get("readabilityScore");
            if (readabilityObj instanceof Number) {
                int readabilityScore = ((Number) readabilityObj).intValue();
                if (readabilityScore >= 60) {
                    score += 5; // Bonus for good readability
                } else if (readabilityScore >= 40) {
                    score += 2; // Small bonus for moderate readability
                }
            }
        }
        
        return Math.min(100, score);
    }
}
