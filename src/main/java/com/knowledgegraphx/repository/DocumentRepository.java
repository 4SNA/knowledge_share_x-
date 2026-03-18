package com.knowledgegraphx.repository;

import com.knowledgegraphx.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByStatusOrderByUploadedAtDesc(String status);

    List<Document> findAllByOrderByUploadedAtDesc();

    @Query("SELECT COUNT(d) FROM Document d WHERE d.status = :status")
    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(d.chunkCount), 0) FROM Document d WHERE d.status = 'INDEXED'")
    long totalIndexedChunks();
}
