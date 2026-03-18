package com.knowledgegraphx.service;

import com.knowledgegraphx.dto.GraphData;
import com.knowledgegraphx.model.Document;
import com.knowledgegraphx.model.DocumentChunk;
import com.knowledgegraphx.repository.DocumentChunkRepository;
import com.knowledgegraphx.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeGraphService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGraphService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;

    private static final String[] DOCUMENT_COLORS = {
            "#6366f1", "#8b5cf6", "#ec4899", "#f43f5e",
            "#f97316", "#eab308", "#22c55e", "#14b8a6",
            "#06b6d4", "#3b82f6"
    };

    public KnowledgeGraphService(DocumentRepository documentRepository,
                                  DocumentChunkRepository chunkRepository,
                                  EmbeddingService embeddingService) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
    }

    /**
     * Build a knowledge graph from indexed documents.
     * Nodes: documents + extracted topics/keywords
     * Links: document-topic relationships based on keyword co-occurrence
     */
    public GraphData buildGraph() {
        List<Document> documents = documentRepository.findByStatusOrderByUploadedAtDesc("INDEXED");
        List<GraphData.GraphNode> nodes = new ArrayList<>();
        List<GraphData.GraphLink> links = new ArrayList<>();

        // Track keyword frequencies across all documents
        Map<String, Integer> globalKeywordFreq = new HashMap<>();
        Map<String, Set<String>> keywordToDocuments = new HashMap<>();

        int colorIndex = 0;

        for (Document doc : documents) {
            String docId = "doc_" + doc.getId();
            String color = DOCUMENT_COLORS[colorIndex % DOCUMENT_COLORS.length];
            colorIndex++;

            // Add document node
            nodes.add(new GraphData.GraphNode(
                    docId,
                    truncate(doc.getFileName(), 30),
                    "document",
                    Math.max(15, Math.min(40, doc.getChunkCount() != null ? doc.getChunkCount() * 3 : 15)),
                    color
            ));

            // Get chunks and extract keywords
            List<DocumentChunk> chunks = chunkRepository.findByDocumentId(doc.getId());
            Set<String> docKeywords = new HashSet<>();

            for (DocumentChunk chunk : chunks) {
                if (chunk.getKeywords() != null) {
                    Arrays.stream(chunk.getKeywords().split(","))
                            .map(String::trim)
                            .filter(k -> !k.isEmpty())
                            .forEach(keyword -> {
                                docKeywords.add(keyword);
                                globalKeywordFreq.merge(keyword, 1, Integer::sum);
                                keywordToDocuments.computeIfAbsent(keyword, k -> new HashSet<>()).add(docId);
                            });
                }
            }
        }

        // Select top keywords as topic nodes (shared across documents preferred)
        List<Map.Entry<String, Integer>> topKeywords = globalKeywordFreq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(30)
                .collect(Collectors.toList());

        for (Map.Entry<String, Integer> entry : topKeywords) {
            String keyword = entry.getKey();
            String keywordId = "topic_" + keyword;
            Set<String> relatedDocs = keywordToDocuments.getOrDefault(keyword, Set.of());

            // Determine if this is a shared topic (appears in multiple docs)
            boolean isShared = relatedDocs.size() > 1;
            int size = Math.max(8, Math.min(25, entry.getValue() * 2));

            nodes.add(new GraphData.GraphNode(
                    keywordId,
                    keyword,
                    isShared ? "topic" : "keyword",
                    size,
                    isShared ? "#f59e0b" : "#64748b"
            ));

            // Create links from documents to this keyword
            for (String docId : relatedDocs) {
                double weight = Math.min(1.0, entry.getValue() / 10.0);
                links.add(new GraphData.GraphLink(
                        docId, keywordId, weight,
                        isShared ? "shared topic" : "keyword"
                ));
            }
        }

        // Add links between documents that share multiple topics
        List<String> docIds = documents.stream()
                .map(d -> "doc_" + d.getId())
                .collect(Collectors.toList());

        for (int i = 0; i < docIds.size(); i++) {
            for (int j = i + 1; j < docIds.size(); j++) {
                String docA = docIds.get(i);
                String docB = docIds.get(j);

                // Count shared topics
                long sharedTopics = keywordToDocuments.values().stream()
                        .filter(docs -> docs.contains(docA) && docs.contains(docB))
                        .count();

                if (sharedTopics >= 2) {
                    links.add(new GraphData.GraphLink(
                            docA, docB,
                            Math.min(1.0, sharedTopics / 5.0),
                            sharedTopics + " shared topics"
                    ));
                }
            }
        }

        log.info("Knowledge graph built: {} nodes, {} links", nodes.size(), links.size());
        return new GraphData(nodes, links);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }
}
