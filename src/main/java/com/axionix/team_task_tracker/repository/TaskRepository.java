package com.axionix.team_task_tracker.repository;

import com.axionix.team_task_tracker.entity.Priority;
import com.axionix.team_task_tracker.entity.Status;
import com.axionix.team_task_tracker.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Objects;

public interface TaskRepository extends JpaRepository<Task,Long> , JpaSpecificationExecutor<Task> {

        long countByProject_Id(Long projectId);

        long countByProject_IdAndStatus(Long projectId, Status status);

        long countByProject_IdAndPriority(Long projectId, Priority priority);
    }
