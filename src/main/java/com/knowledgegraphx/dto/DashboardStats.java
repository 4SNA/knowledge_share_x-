package com.knowledgegraphx.dto;

public class DashboardStats {

    private long totalDocuments;
    private long indexedDocuments;
    private long totalChunks;
    private long totalQueries;
    private long processingDocuments;

    // Getters and Setters
    public long getTotalDocuments() { return totalDocuments; }
    public void setTotalDocuments(long totalDocuments) { this.totalDocuments = totalDocuments; }

    public long getIndexedDocuments() { return indexedDocuments; }
    public void setIndexedDocuments(long indexedDocuments) { this.indexedDocuments = indexedDocuments; }

    public long getTotalChunks() { return totalChunks; }
    public void setTotalChunks(long totalChunks) { this.totalChunks = totalChunks; }

    public long getTotalQueries() { return totalQueries; }
    public void setTotalQueries(long totalQueries) { this.totalQueries = totalQueries; }

    public long getProcessingDocuments() { return processingDocuments; }
    public void setProcessingDocuments(long processingDocuments) { this.processingDocuments = processingDocuments; }
}
