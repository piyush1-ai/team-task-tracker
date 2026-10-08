package com.axionix.team_task_tracker.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectDetailResponse(Long id , String name,
                                    String description , Long owenerId , LocalDateTime createdAt,
                                    List<UserResponse> members)  { }
