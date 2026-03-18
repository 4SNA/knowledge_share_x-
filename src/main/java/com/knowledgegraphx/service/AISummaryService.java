package com.knowledgegraphx.service;

import com.knowledgegraphx.model.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AISummaryService {

    private static final Logger log = LoggerFactory.getLogger(AISummaryService.class);

    private final EmbeddingService embeddingService;

    public AISummaryService(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    /**
     * Generate a contextual AI summary from relevant document chunks for a given query.
     * Uses extractive summarization with sentence ranking by query relevance.
     */
    public String generateSummary(String query, List<VectorSearchService.ScoredChunk> scoredChunks) {
        if (scoredChunks == null || scoredChunks.isEmpty()) {
            return "I couldn't find any relevant information in the knowledge base for your query. " +
                    "Please try rephrasing your question or upload relevant documents first.";
        }

        List<String> queryTokens = embeddingService.tokenize(query);

        // Extract and rank sentences from top chunks
        List<ScoredSentence> allSentences = new ArrayList<>();

        for (VectorSearchService.ScoredChunk scoredChunk : scoredChunks) {
            String content = scoredChunk.getChunk().getContent();
            String documentName = scoredChunk.getChunk().getDocument().getFileName();
            String[] sentences = content.split("(?<=[.!?])\\s+");

            for (String sentence : sentences) {
                if (sentence.trim().length() < 15) continue;

                double relevance = computeSentenceRelevance(sentence, queryTokens);
                // Boost by chunk's overall relevance score
                relevance *= (1.0 + scoredChunk.getScore());

                allSentences.add(new ScoredSentence(sentence.trim(), relevance, documentName));
            }
        }

        // Sort by relevance and select top sentences
        allSentences.sort(Comparator.comparingDouble(ScoredSentence::getScore).reversed());

        // Build summary from top sentences
        StringBuilder summary = new StringBuilder();
        Set<String> usedSentences = new HashSet<>();
        int sentenceCount = 0;
        int maxSentences = 8;

        // Start with a brief intro
        summary.append("Based on the available knowledge base, here is what I found:\n\n");

        for (ScoredSentence ss : allSentences) {
            if (sentenceCount >= maxSentences) break;

            // Avoid near-duplicate sentences
            String normalized = ss.sentence.toLowerCase().replaceAll("\\s+", " ");
            boolean isDuplicate = usedSentences.stream()
                    .anyMatch(used -> computeJaccardSimilarity(used, normalized) > 0.7);

            if (!isDuplicate) {
                summary.append(ss.sentence);
                if (!ss.sentence.endsWith(".") && !ss.sentence.endsWith("!") && !ss.sentence.endsWith("?")) {
                    summary.append(".");
                }
                summary.append(" ");
                usedSentences.add(normalized);
                sentenceCount++;
            }
        }

        if (sentenceCount == 0) {
            summary.append("The documents contain information related to your query, but I couldn't extract specific sentences. ");
            summary.append("Try asking a more specific question.");
        }

        log.info("Generated summary with {} sentences for query: '{}'", sentenceCount, query);
        return summary.toString().trim();
    }

    /**
     * Generate smart query suggestions based on document content and query history.
     */
    public List<String> generateSuggestions(String query, List<VectorSearchService.ScoredChunk> scoredChunks) {
        List<String> suggestions = new ArrayList<>();

        if (scoredChunks != null && !scoredChunks.isEmpty()) {
            // Extract top keywords from relevant chunks
            Set<String> allKeywords = new LinkedHashSet<>();
            for (VectorSearchService.ScoredChunk sc : scoredChunks) {
                List<String> keywords = embeddingService.extractKeywords(sc.getChunk().getContent(), 5);
                allKeywords.addAll(keywords);
            }

            List<String> topKeywords = new ArrayList<>(allKeywords);
            if (topKeywords.size() > 3) topKeywords = topKeywords.subList(0, 3);

            // Generate suggestion questions
            for (String keyword : topKeywords) {
                suggestions.add("What are the key details about " + keyword + "?");
            }

            if (suggestions.size() < 3) {
                suggestions.add("Can you summarize the main themes in the documents?");
                suggestions.add("What are the most important findings?");
            }
        } else {
            suggestions.add("What topics are covered in the knowledge base?");
            suggestions.add("Summarize the uploaded documents");
            suggestions.add("What are the key findings?");
        }

        return suggestions.stream().limit(3).collect(Collectors.toList());
    }

    private double computeSentenceRelevance(String sentence, List<String> queryTokens) {
        List<String> sentenceTokens = embeddingService.tokenize(sentence);
        if (sentenceTokens.isEmpty() || queryTokens.isEmpty()) return 0;

        Set<String> sentenceSet = new HashSet<>(sentenceTokens);
        long matchCount = queryTokens.stream().filter(sentenceSet::contains).count();

        // Combine token overlap with sentence length preference
        double tokenOverlap = (double) matchCount / queryTokens.size();
        double lengthPenalty = Math.min(1.0, sentenceTokens.size() / 10.0);

        return tokenOverlap * 0.7 + lengthPenalty * 0.3;
    }

    private double computeJaccardSimilarity(String a, String b) {
        Set<String> setA = new HashSet<>(Arrays.asList(a.split("\\s+")));
        Set<String> setB = new HashSet<>(Arrays.asList(b.split("\\s+")));
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private static class ScoredSentence {
        final String sentence;
        final double score;
        final String documentName;

        ScoredSentence(String sentence, double score, String documentName) {
            this.sentence = sentence;
            this.score = score;
            this.documentName = documentName;
        }

        double getScore() { return score; }
    }
}
