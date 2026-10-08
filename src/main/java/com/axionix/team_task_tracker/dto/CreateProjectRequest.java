package com.axionix.team_task_tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(@NotBlank @Size(max = 150) String name,
                                   @Size(max = 500) String description) {
}
