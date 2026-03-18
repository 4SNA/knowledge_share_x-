package com.knowledgegraphx.controller;

import com.knowledgegraphx.dto.QueryRequest;
import com.knowledgegraphx.dto.QueryResponseDTO;
import com.knowledgegraphx.model.QueryHistory;
import com.knowledgegraphx.service.QueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<?> submitQuery(@RequestBody QueryRequest request) {
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Query cannot be empty"));
        }

        QueryResponseDTO response = queryService.processQuery(request.getQuery().trim());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<QueryHistory>> getHistory() {
        return ResponseEntity.ok(queryService.getHistory());
    }

    @GetMapping("/history/recent")
    public ResponseEntity<List<QueryHistory>> getRecentHistory() {
        return ResponseEntity.ok(queryService.getRecentHistory());
    }

    @GetMapping("/history/search")
    public ResponseEntity<List<QueryHistory>> searchHistory(@RequestParam String keyword) {
        return ResponseEntity.ok(queryService.searchHistory(keyword));
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<?> deleteHistory(@PathVariable Long id) {
        if (queryService.deleteHistory(id)) {
            return ResponseEntity.ok(Map.of("message", "History entry deleted"));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/history")
    public ResponseEntity<?> clearHistory() {
        queryService.clearHistory();
        return ResponseEntity.ok(Map.of("message", "All history cleared"));
    }
}
