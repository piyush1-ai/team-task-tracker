package com.axionix.team_task_tracker.repository;

import com.axionix.team_task_tracker.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
