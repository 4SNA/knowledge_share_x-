package com.knowledgegraphx.controller;

import com.knowledgegraphx.dto.GraphData;
import com.knowledgegraphx.service.KnowledgeGraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/graph")
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;

    public KnowledgeGraphController(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    @GetMapping
    public ResponseEntity<GraphData> getGraph() {
        return ResponseEntity.ok(knowledgeGraphService.buildGraph());
    }
}
