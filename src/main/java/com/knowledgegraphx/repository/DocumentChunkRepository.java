package com.knowledgegraphx.repository;

import com.knowledgegraphx.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentId(Long documentId);

    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(Long documentId);

    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.embedding IS NOT NULL")
    List<DocumentChunk> findAllWithEmbeddings();

    void deleteByDocumentId(Long documentId);

    long countByDocumentId(Long documentId);

    @Query("SELECT dc.keywords FROM DocumentChunk dc WHERE dc.keywords IS NOT NULL")
    List<String> findAllKeywords();
}
