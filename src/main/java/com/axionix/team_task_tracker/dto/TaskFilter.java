package com.axionix.team_task_tracker.dto;

import com.axionix.team_task_tracker.entity.Priority;
import com.axionix.team_task_tracker.entity.Status;
import java.time.LocalDateTime;

public record TaskFilter(Status status, Priority priority ,
                         Long assigneeId , LocalDateTime dueFrom , LocalDateTime dueTo) {}
