package com.knowledgegraphx.service;

import com.knowledgegraphx.dto.DocumentDTO;
import com.knowledgegraphx.model.Document;
import com.knowledgegraphx.model.DocumentChunk;
import com.knowledgegraphx.repository.DocumentChunkRepository;
import com.knowledgegraphx.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final TextExtractionService textExtractionService;
    private final EmbeddingService embeddingService;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentChunkRepository chunkRepository,
                           TextExtractionService textExtractionService,
                           EmbeddingService embeddingService) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.textExtractionService = textExtractionService;
        this.embeddingService = embeddingService;
    }

    /**
     * Upload and process a document: extract text, chunk, embed, and store.
     */
    @Transactional
    public DocumentDTO uploadAndProcess(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String fileType = getFileExtension(fileName);

        // Create document record
        Document doc = new Document();
        doc.setFileName(fileName);
        doc.setFileType(fileType);
        doc.setFileSize(file.getSize());
        doc.setStatus("PROCESSING");
        doc = documentRepository.save(doc);

        log.info("Processing document: {} (ID: {}, type: {}, size: {} bytes)",
                fileName, doc.getId(), fileType, file.getSize());

        try {
            // Extract text
            String text = textExtractionService.extractText(file);

            if (text == null || text.isBlank()) {
                doc.setStatus("FAILED");
                doc.setSummary("No text could be extracted from the document.");
                documentRepository.save(doc);
                return toDTO(doc);
            }

            // Update vocabulary with this document
            embeddingService.updateVocabulary(List.of(text));

            // Chunk text
            List<String> chunks = embeddingService.chunkText(text);

            // Create chunks with embeddings
            List<DocumentChunk> savedChunks = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocument(doc);
                chunk.setContent(chunks.get(i));
                chunk.setChunkIndex(i);

                // Compute and store embedding
                double[] embedding = embeddingService.computeEmbedding(chunks.get(i));
                chunk.setEmbedding(embeddingService.embeddingToString(embedding));

                // Extract keywords
                List<String> keywords = embeddingService.extractKeywords(chunks.get(i), 10);
                chunk.setKeywords(String.join(",", keywords));

                savedChunks.add(chunk);
            }

            chunkRepository.saveAll(savedChunks);

            // Update document status
            doc.setStatus("INDEXED");
            doc.setChunkCount(chunks.size());
            doc.setProcessedAt(LocalDateTime.now());

            // Generate document summary
            String summary = generateDocSummary(text);
            doc.setSummary(summary);

            doc = documentRepository.save(doc);
            log.info("Document indexed successfully: {} ({} chunks)", fileName, chunks.size());

            // Re-embed existing chunks with updated vocabulary (best effort)
            reindexExistingChunks(doc.getId());

        } catch (Exception e) {
            log.error("Failed to process document: {}", fileName, e);
            doc.setStatus("FAILED");
            doc.setSummary("Processing failed: " + e.getMessage());
            documentRepository.save(doc);
        }

        return toDTO(doc);
    }

    /**
     * Get all documents as DTOs.
     */
    public List<DocumentDTO> getAllDocuments() {
        return documentRepository.findAllByOrderByUploadedAtDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a document by ID.
     */
    public DocumentDTO getDocument(Long id) {
        return documentRepository.findById(id)
                .map(this::toDTO)
                .orElse(null);
    }

    /**
     * Delete a document and its chunks.
     */
    @Transactional
    public boolean deleteDocument(Long id) {
        if (documentRepository.existsById(id)) {
            chunkRepository.deleteByDocumentId(id);
            documentRepository.deleteById(id);
            log.info("Deleted document ID: {}", id);
            return true;
        }
        return false;
    }

    /**
     * Re-index all existing chunks when vocabulary changes.
     */
    private void reindexExistingChunks(Long excludeDocId) {
        try {
            List<DocumentChunk> allChunks = chunkRepository.findAll();
            for (DocumentChunk chunk : allChunks) {
                if (!chunk.getDocument().getId().equals(excludeDocId)) {
                    double[] embedding = embeddingService.computeEmbedding(chunk.getContent());
                    chunk.setEmbedding(embeddingService.embeddingToString(embedding));
                }
            }
            chunkRepository.saveAll(allChunks);
            log.info("Re-indexed {} existing chunks with updated vocabulary", allChunks.size());
        } catch (Exception e) {
            log.warn("Failed to re-index existing chunks: {}", e.getMessage());
        }
    }

    private String generateDocSummary(String text) {
        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder summary = new StringBuilder();
        int count = 0;
        for (String sentence : sentences) {
            if (sentence.trim().length() > 20 && count < 3) {
                summary.append(sentence.trim()).append(" ");
                count++;
            }
        }
        String result = summary.toString().trim();
        if (result.length() > 500) result = result.substring(0, 497) + "...";
        return result.isEmpty() ? "Document processed successfully." : result;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "unknown";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot >= 0 ? fileName.substring(lastDot + 1).toLowerCase() : "unknown";
    }

    private DocumentDTO toDTO(Document doc) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(doc.getId());
        dto.setFileName(doc.getFileName());
        dto.setFileType(doc.getFileType());
        dto.setFileSize(doc.getFileSize());
        dto.setStatus(doc.getStatus());
        dto.setChunkCount(doc.getChunkCount());
        dto.setSummary(doc.getSummary());
        dto.setUploadedAt(doc.getUploadedAt());
        dto.setProcessedAt(doc.getProcessedAt());
        return dto;
    }
}
