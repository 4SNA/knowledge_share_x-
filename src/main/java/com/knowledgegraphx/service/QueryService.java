package com.knowledgegraphx.service;

import com.knowledgegraphx.dto.QueryResponseDTO;
import com.knowledgegraphx.model.QueryHistory;
import com.knowledgegraphx.repository.QueryHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QueryService {

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);

    private final VectorSearchService vectorSearchService;
    private final AISummaryService aiSummaryService;
    private final QueryHistoryRepository queryHistoryRepository;

    public QueryService(VectorSearchService vectorSearchService,
                        AISummaryService aiSummaryService,
                        QueryHistoryRepository queryHistoryRepository) {
        this.vectorSearchService = vectorSearchService;
        this.aiSummaryService = aiSummaryService;
        this.queryHistoryRepository = queryHistoryRepository;
    }

    /**
     * Process a natural language query against the knowledge base.
     */
    public QueryResponseDTO processQuery(String queryText) {
        log.info("Processing query: '{}'", queryText);

        // Search for relevant chunks
        List<VectorSearchService.ScoredChunk> results = vectorSearchService.search(queryText, 5);

        // Generate AI summary
        String answer = aiSummaryService.generateSummary(queryText, results);

        // Generate suggestions
        List<String> suggestions = aiSummaryService.generateSuggestions(queryText, results);

        // Build source references
        List<QueryResponseDTO.SourceReference> sources = results.stream()
                .map(sc -> {
                    String content = sc.getChunk().getContent();
                    String excerpt = content.length() > 200 ? content.substring(0, 197) + "..." : content;
                    return new QueryResponseDTO.SourceReference(
                            sc.getChunk().getDocument().getFileName(),
                            sc.getChunk().getChunkIndex(),
                            excerpt,
                            Math.round(sc.getScore() * 10000.0) / 10000.0
                    );
                })
                .collect(Collectors.toList());

        // Compute confidence score
        double avgScore = results.stream()
                .mapToDouble(VectorSearchService.ScoredChunk::getScore)
                .average()
                .orElse(0);
        double confidence = Math.min(1.0, avgScore * 2); // Scale up for presentation

        // Build response
        QueryResponseDTO response = new QueryResponseDTO();
        response.setQuery(queryText);
        response.setAnswer(answer);
        response.setConfidenceScore(Math.round(confidence * 100.0) / 100.0);
        response.setSources(sources);
        response.setSuggestions(suggestions);

        // Save to history
        saveToHistory(queryText, answer, sources, results.size(), confidence);

        log.info("Query processed. Confidence: {}, Sources: {}", confidence, sources.size());
        return response;
    }

    /**
     * Get all query history.
     */
    public List<QueryHistory> getHistory() {
        return queryHistoryRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Get recent query history (last 10).
     */
    public List<QueryHistory> getRecentHistory() {
        return queryHistoryRepository.findTop10ByOrderByCreatedAtDesc();
    }

    /**
     * Search query history.
     */
    public List<QueryHistory> searchHistory(String keyword) {
        return queryHistoryRepository.findByQueryTextContainingIgnoreCaseOrderByCreatedAtDesc(keyword);
    }

    /**
     * Delete a query history entry.
     */
    public boolean deleteHistory(Long id) {
        if (queryHistoryRepository.existsById(id)) {
            queryHistoryRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Clear all query history.
     */
    public void clearHistory() {
        queryHistoryRepository.deleteAll();
    }

    private void saveToHistory(String query, String answer,
                                List<QueryResponseDTO.SourceReference> sources,
                                int relevantChunks, double confidence) {
        QueryHistory history = new QueryHistory();
        history.setQueryText(query);
        history.setResponse(answer);
        history.setRelevantChunks(relevantChunks);
        history.setConfidenceScore(confidence);

        // Serialize sources
        String sourcesStr = sources.stream()
                .map(s -> s.getDocumentName() + " (chunk " + s.getChunkIndex() + ")")
                .collect(Collectors.joining(", "));
        history.setSources(sourcesStr);

        queryHistoryRepository.save(history);
    }
}
