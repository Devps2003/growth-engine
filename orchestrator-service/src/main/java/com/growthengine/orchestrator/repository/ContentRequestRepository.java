package com.growthengine.orchestrator.repository;

import com.growthengine.orchestrator.entity.ContentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentRequestRepository extends JpaRepository<ContentRequest, Long> {
    // JpaRepository provides: save(), findById(), findAll(), delete(), etc.
    // No need to write implementation - Spring generates it!
}