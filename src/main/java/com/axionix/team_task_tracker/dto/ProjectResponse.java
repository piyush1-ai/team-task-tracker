package com.axionix.team_task_tracker.dto;

import java.time.LocalDateTime;

public record ProjectResponse(Long id, String name, String description,
                              Long ownerId, LocalDateTime createdAt) {
}
