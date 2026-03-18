package com.knowledgegraphx.service;

import com.knowledgegraphx.model.DocumentChunk;
import com.knowledgegraphx.repository.DocumentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);

    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;

    public VectorSearchService(DocumentChunkRepository chunkRepository, EmbeddingService embeddingService) {
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
    }

    /**
     * Search for the most relevant document chunks given a query text.
     * Returns chunks sorted by cosine similarity in descending order.
     */
    public List<ScoredChunk> search(String queryText, int topK) {
        if (!embeddingService.isVocabularyReady()) {
            log.warn("Vocabulary not ready. Falling back to keyword search.");
            return keywordFallbackSearch(queryText, topK);
        }

        double[] queryEmbedding = embeddingService.computeEmbedding(queryText);
        List<DocumentChunk> allChunks = chunkRepository.findAllWithEmbeddings();

        List<ScoredChunk> scored = allChunks.stream()
                .map(chunk -> {
                    double[] chunkEmbedding = embeddingService.stringToEmbedding(chunk.getEmbedding());
                    double similarity = embeddingService.cosineSimilarity(queryEmbedding, chunkEmbedding);
                    return new ScoredChunk(chunk, similarity);
                })
                .filter(sc -> sc.score > 0.01)
                .sorted(Comparator.comparingDouble(ScoredChunk::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());

        log.info("Vector search for '{}' returned {} results (top score: {})",
                queryText, scored.size(),
                scored.isEmpty() ? "N/A" : String.format("%.4f", scored.get(0).score));

        return scored;
    }

    /**
     * Fallback: simple keyword matching when vocabulary isn't ready.
     */
    private List<ScoredChunk> keywordFallbackSearch(String queryText, int topK) {
        List<String> queryTokens = embeddingService.tokenize(queryText);
        List<DocumentChunk> allChunks = chunkRepository.findAll();

        return allChunks.stream()
                .map(chunk -> {
                    String content = chunk.getContent().toLowerCase();
                    long matchCount = queryTokens.stream()
                            .filter(token -> content.contains(token))
                            .count();
                    double score = queryTokens.isEmpty() ? 0 : (double) matchCount / queryTokens.size();
                    return new ScoredChunk(chunk, score);
                })
                .filter(sc -> sc.score > 0)
                .sorted(Comparator.comparingDouble(ScoredChunk::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * Container for a chunk with its relevance score.
     */
    public static class ScoredChunk {
        private final DocumentChunk chunk;
        private final double score;

        public ScoredChunk(DocumentChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }

        public DocumentChunk getChunk() { return chunk; }
        public double getScore() { return score; }
    }
}
