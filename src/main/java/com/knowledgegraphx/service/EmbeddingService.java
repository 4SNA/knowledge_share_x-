package com.knowledgegraphx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    @Value("${app.embedding.chunk-size:500}")
    private int chunkSize;

    @Value("${app.embedding.chunk-overlap:50}")
    private int chunkOverlap;

    @Value("${app.embedding.vocabulary-size:5000}")
    private int vocabularySize;

    private final Map<String, Integer> vocabulary = new ConcurrentHashMap<>();
    private final Map<String, Double> idfScores = new ConcurrentHashMap<>();
    private int totalDocuments = 0;
    private final Map<String, Integer> documentFrequencies = new ConcurrentHashMap<>();

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "shall",
            "should", "may", "might", "must", "can", "could", "and", "but", "or",
            "nor", "not", "so", "yet", "both", "either", "neither", "each", "every",
            "all", "any", "few", "more", "most", "other", "some", "such", "no",
            "only", "own", "same", "than", "too", "very", "just", "because",
            "as", "until", "while", "of", "at", "by", "for", "with", "about",
            "against", "between", "through", "during", "before", "after", "above",
            "below", "to", "from", "up", "down", "in", "out", "on", "off", "over",
            "under", "again", "further", "then", "once", "here", "there", "when",
            "where", "why", "how", "this", "that", "these", "those", "i", "me",
            "my", "myself", "we", "our", "ours", "ourselves", "you", "your",
            "yours", "yourself", "yourselves", "he", "him", "his", "himself",
            "she", "her", "hers", "herself", "it", "its", "itself", "they",
            "them", "their", "theirs", "themselves", "what", "which", "who",
            "whom", "if", "into", "also", "like", "get", "got", "well"
    );

    /**
     * Split text into overlapping chunks for embedding.
     */
    public List<String> chunkText(String text) {
        if (text == null || text.isBlank()) return List.of();

        List<String> chunks = new ArrayList<>();
        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                // Keep overlap
                String overlap = currentChunk.substring(
                        Math.max(0, currentChunk.length() - chunkOverlap));
                currentChunk = new StringBuilder(overlap);
            }
            currentChunk.append(sentence).append(" ");
        }
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        log.info("Split text into {} chunks (chunk size: {}, overlap: {})", chunks.size(), chunkSize, chunkOverlap);
        return chunks;
    }

    /**
     * Tokenize text into normalized word tokens, removing stop words.
     */
    public List<String> tokenize(String text) {
        if (text == null) return List.of();
        return Arrays.stream(text.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", " ").split("\\s+"))
                .filter(word -> word.length() > 2)
                .filter(word -> !STOP_WORDS.contains(word))
                .collect(Collectors.toList());
    }

    /**
     * Update the global vocabulary and IDF scores with new document text.
     */
    public void updateVocabulary(List<String> documentTexts) {
        totalDocuments += documentTexts.size();

        for (String text : documentTexts) {
            Set<String> uniqueTokens = new HashSet<>(tokenize(text));
            for (String token : uniqueTokens) {
                documentFrequencies.merge(token, 1, Integer::sum);
            }
        }

        // Rebuild vocabulary: keep top N by document frequency
        List<Map.Entry<String, Integer>> sorted = documentFrequencies.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(vocabularySize)
                .collect(Collectors.toList());

        vocabulary.clear();
        idfScores.clear();
        for (int i = 0; i < sorted.size(); i++) {
            String term = sorted.get(i).getKey();
            vocabulary.put(term, i);
            double idf = Math.log((double) (totalDocuments + 1) / (sorted.get(i).getValue() + 1)) + 1.0;
            idfScores.put(term, idf);
        }

        log.info("Vocabulary updated: {} terms, {} documents", vocabulary.size(), totalDocuments);
    }

    /**
     * Compute TF-IDF embedding vector for a given text.
     */
    public double[] computeEmbedding(String text) {
        double[] vector = new double[vocabularySize];
        List<String> tokens = tokenize(text);

        if (tokens.isEmpty()) return vector;

        // Compute term frequencies
        Map<String, Long> termFreqs = tokens.stream()
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        double maxFreq = termFreqs.values().stream().mapToLong(Long::longValue).max().orElse(1);

        for (Map.Entry<String, Long> entry : termFreqs.entrySet()) {
            Integer index = vocabulary.get(entry.getKey());
            if (index != null && index < vocabularySize) {
                double tf = 0.5 + 0.5 * (entry.getValue() / maxFreq); // Augmented TF
                double idf = idfScores.getOrDefault(entry.getKey(), 1.0);
                vector[index] = tf * idf;
            }
        }

        // L2 normalize
        double norm = 0;
        for (double v : vector) norm += v * v;
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) vector[i] /= norm;
        }

        return vector;
    }

    /**
     * Serialize embedding to a compact string format.
     */
    public String embeddingToString(double[] embedding) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (double v : embedding) {
            if (v != 0) {
                if (!first) sb.append(",");
                sb.append(String.format("%d:%.6f",
                        Arrays.asList(embedding).indexOf(v) == -1 ? 0 : indexOf(embedding, v), v));
                first = false;
            }
        }
        // Use sparse format: index:value,...
        sb = new StringBuilder();
        first = true;
        for (int i = 0; i < embedding.length; i++) {
            if (embedding[i] != 0) {
                if (!first) sb.append(",");
                sb.append(i).append(":").append(String.format("%.6f", embedding[i]));
                first = false;
            }
        }
        return sb.toString();
    }

    /**
     * Deserialize embedding from sparse string format.
     */
    public double[] stringToEmbedding(String str) {
        double[] vector = new double[vocabularySize];
        if (str == null || str.isBlank()) return vector;

        String[] parts = str.split(",");
        for (String part : parts) {
            String[] kv = part.split(":");
            if (kv.length == 2) {
                try {
                    int index = Integer.parseInt(kv[0].trim());
                    double value = Double.parseDouble(kv[1].trim());
                    if (index >= 0 && index < vocabularySize) {
                        vector[index] = value;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return vector;
    }

    /**
     * Compute cosine similarity between two vectors.
     */
    public double cosineSimilarity(double[] a, double[] b) {
        double dot = 0, normA = 0, normB = 0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0 : dot / denom;
    }

    /**
     * Extract top keywords from text based on TF-IDF scores.
     */
    public List<String> extractKeywords(String text, int topN) {
        List<String> tokens = tokenize(text);
        Map<String, Long> termFreqs = tokens.stream()
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        return termFreqs.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public int getVocabularySize() { return vocabularySize; }

    public boolean isVocabularyReady() { return !vocabulary.isEmpty(); }

    private int indexOf(double[] arr, double value) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == value) return i;
        }
        return -1;
    }
}
