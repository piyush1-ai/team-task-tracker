package com.axionix.team_task_tracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AddMemberRequest(@NotBlank @Email String email) {
}
