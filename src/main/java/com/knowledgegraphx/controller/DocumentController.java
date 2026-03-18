package com.knowledgegraphx.controller;

import com.knowledgegraphx.dto.DocumentDTO;
import com.knowledgegraphx.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Please select a file to upload"));
            }

            String fileName = file.getOriginalFilename();
            if (fileName != null) {
                String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
                if (!List.of("pdf", "docx", "csv", "txt", "md").contains(ext)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Unsupported file type: " + ext +
                                    ". Supported: PDF, DOCX, CSV, TXT, MD"));
                }
            }

            log.info("Receiving upload: {} ({} bytes)", file.getOriginalFilename(), file.getSize());
            DocumentDTO result = documentService.uploadAndProcess(file);
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            log.error("Upload failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process file: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<DocumentDTO>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAllDocuments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDocument(@PathVariable Long id) {
        DocumentDTO doc = documentService.getDocument(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(doc);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        if (documentService.deleteDocument(id)) {
            return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
        }
        return ResponseEntity.notFound().build();
    }
}
