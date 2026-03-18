package com.knowledgegraphx.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "query_history")
public class QueryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String queryText;

    @Column(columnDefinition = "CLOB")
    private String response;

    @Column(length = 5000)
    private String sources;

    private Integer relevantChunks;

    private Double confidenceScore;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQueryText() { return queryText; }
    public void setQueryText(String queryText) { this.queryText = queryText; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public String getSources() { return sources; }
    public void setSources(String sources) { this.sources = sources; }

    public Integer getRelevantChunks() { return relevantChunks; }
    public void setRelevantChunks(Integer relevantChunks) { this.relevantChunks = relevantChunks; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
