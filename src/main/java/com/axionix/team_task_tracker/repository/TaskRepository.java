package com.axionix.team_task_tracker.repository;

import com.axionix.team_task_tracker.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task,Long> {
}
