package com.knowledgegraphx.controller;

import com.knowledgegraphx.dto.DashboardStats;
import com.knowledgegraphx.repository.DocumentRepository;
import com.knowledgegraphx.repository.QueryHistoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DocumentRepository documentRepository;
    private final QueryHistoryRepository queryHistoryRepository;

    public DashboardController(DocumentRepository documentRepository,
                                QueryHistoryRepository queryHistoryRepository) {
        this.documentRepository = documentRepository;
        this.queryHistoryRepository = queryHistoryRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats() {
        DashboardStats stats = new DashboardStats();
        stats.setTotalDocuments(documentRepository.count());
        stats.setIndexedDocuments(documentRepository.countByStatus("INDEXED"));
        stats.setTotalChunks(documentRepository.totalIndexedChunks());
        stats.setTotalQueries(queryHistoryRepository.count());
        stats.setProcessingDocuments(documentRepository.countByStatus("PROCESSING"));
        return ResponseEntity.ok(stats);
    }
}
