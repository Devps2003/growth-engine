package com.growthengine.agent.writer.repository;

import com.growthengine.agent.writer.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // JpaRepository provides: save(), findById(), findAll(), delete(), etc.
}

