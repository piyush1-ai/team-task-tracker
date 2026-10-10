package com.axionix.team_task_tracker.dto;

import com.axionix.team_task_tracker.entity.Priority;
import com.axionix.team_task_tracker.entity.Status;

import java.time.LocalDateTime;

public record TaskResponse(Long id, Long projectId, String title, String description,
                           Priority priority, Status status, LocalDateTime dueDate,
                           Long assigneeId, Long createdById,
                           LocalDateTime createdAt, LocalDateTime updatedAt,
                           LocalDateTime completedAt) {}

