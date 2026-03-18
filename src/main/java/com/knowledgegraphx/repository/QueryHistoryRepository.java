package com.knowledgegraphx.repository;

import com.knowledgegraphx.model.QueryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {

    List<QueryHistory> findAllByOrderByCreatedAtDesc();

    List<QueryHistory> findTop10ByOrderByCreatedAtDesc();

    List<QueryHistory> findByQueryTextContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);
}
