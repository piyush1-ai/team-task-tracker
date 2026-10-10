package com.axionix.team_task_tracker.dto;

import com.axionix.team_task_tracker.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 1000) String description,
        @NotNull Priority priority,
        @NotNull LocalDateTime dueDate,
        Long assigneeId ){}

