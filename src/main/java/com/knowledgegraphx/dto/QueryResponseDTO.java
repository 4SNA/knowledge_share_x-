package com.knowledgegraphx.dto;

import java.util.List;

public class QueryResponseDTO {

    private String query;
    private String answer;
    private double confidenceScore;
    private List<SourceReference> sources;
    private List<String> suggestions;

    public static class SourceReference {
        private String documentName;
        private int chunkIndex;
        private String excerpt;
        private double relevanceScore;

        public SourceReference() {}

        public SourceReference(String documentName, int chunkIndex, String excerpt, double relevanceScore) {
            this.documentName = documentName;
            this.chunkIndex = chunkIndex;
            this.excerpt = excerpt;
            this.relevanceScore = relevanceScore;
        }

        public String getDocumentName() { return documentName; }
        public void setDocumentName(String documentName) { this.documentName = documentName; }

        public int getChunkIndex() { return chunkIndex; }
        public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

        public String getExcerpt() { return excerpt; }
        public void setExcerpt(String excerpt) { this.excerpt = excerpt; }

        public double getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }
    }

    // Getters and Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public List<SourceReference> getSources() { return sources; }
    public void setSources(List<SourceReference> sources) { this.sources = sources; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
}
