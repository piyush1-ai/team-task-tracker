package com.axionix.team_task_tracker.dto;

import com.axionix.team_task_tracker.entity.Priority;
import com.axionix.team_task_tracker.entity.Status;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateTaskRequest(
        @Size(max = 200) String title,
        @Size(max = 1000) String description,
        Priority priority,
        Status status,
        LocalDateTime dueDate,
        Long assigneeId) {}

