package com.growthengine.agent.evaluator.service;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;

@Service
public class ReadabilityCalculator {
    
    /**
     * Calculates Flesch Reading Ease score (0-100, higher = easier to read).
     * 
     * Formula: 206.835 - (1.015 × ASL) - (84.6 × ASW)
     * ASL = Average Sentence Length (words per sentence)
     * ASW = Average Syllables per Word
     */
    public double calculateFleschReadingEase(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }
        
        // Count sentences (split by . ! ?)
        String[] sentences = text.split("[.!?]+");
        int sentenceCount = sentences.length;
        if (sentenceCount == 0) sentenceCount = 1;
        
        // Count words
        String[] words = text.split("\\s+");
        int wordCount = words.length;
        if (wordCount == 0) return 0.0;
        
        // Count syllables
        int totalSyllables = 0;
        for (String word : words) {
            totalSyllables += countSyllables(word);
        }
        
        // Calculate averages
        double avgSentenceLength = (double) wordCount / sentenceCount;
        double avgSyllablesPerWord = (double) totalSyllables / wordCount;
        
        // Flesch Reading Ease formula
        double score = 206.835 - (1.015 * avgSentenceLength) - (84.6 * avgSyllablesPerWord);
        
        // Clamp to 0-100
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * Calculates Flesch-Kincaid Grade Level (US school grade level).
     * 
     * Formula: (0.39 × ASL) + (11.8 × ASW) - 15.59
     */
    public double calculateFleschKincaidGradeLevel(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }
        
        String[] sentences = text.split("[.!?]+");
        int sentenceCount = sentences.length;
        if (sentenceCount == 0) sentenceCount = 1;
        
        String[] words = text.split("\\s+");
        int wordCount = words.length;
        if (wordCount == 0) return 0.0;
        
        int totalSyllables = 0;
        for (String word : words) {
            totalSyllables += countSyllables(word);
        }
        
        double avgSentenceLength = (double) wordCount / sentenceCount;
        double avgSyllablesPerWord = (double) totalSyllables / wordCount;
        
        // Flesch-Kincaid Grade Level formula
        double gradeLevel = (0.39 * avgSentenceLength) + (11.8 * avgSyllablesPerWord) - 15.59;
        
        return Math.max(0, gradeLevel);
    }
    
    /**
     * Counts syllables in a word.
     * Simple heuristic: count vowel groups.
     */
    private int countSyllables(String word) {
        if (word == null || word.isEmpty()) {
            return 1;
        }
        
        word = word.toLowerCase();
        
        // Remove silent 'e' at the end
        if (word.endsWith("e")) {
            word = word.substring(0, word.length() - 1);
        }
        
        // Count vowel groups
        int syllables = 0;
        boolean previousWasVowel = false;
        
        for (char c : word.toCharArray()) {
            boolean isVowel = (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u' || c == 'y');
            
            if (isVowel && !previousWasVowel) {
                syllables++;
            }
            previousWasVowel = isVowel;
        }
        
        // At least one syllable
        return Math.max(1, syllables);
    }
    
    /**
     * Converts Flesch Reading Ease to a 0-100 score for our system.
     * 90-100 = Very Easy (score: 90-100)
     * 80-89 = Easy (score: 80-89)
     * 70-79 = Fairly Easy (score: 70-79)
     * 60-69 = Standard (score: 60-69)
     * 50-59 = Fairly Difficult (score: 50-59)
     * 30-49 = Difficult (score: 30-49)
     * 0-29 = Very Difficult (score: 0-29)
     */
    public int convertToReadabilityScore(double fleschScore) {
        // Flesch score is already 0-100, but we normalize it
        // Higher Flesch = easier to read = higher score
        return (int) Math.round(fleschScore);
    }
}