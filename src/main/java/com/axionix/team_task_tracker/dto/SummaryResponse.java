package com.axionix.team_task_tracker.dto;

import com.axionix.team_task_tracker.entity.Priority;
import com.axionix.team_task_tracker.entity.Status;

import java.util.Map;

public record SummaryResponse (long total ,
                               Map<Status , Long> byStatus,
                               Map<Priority , Long> byPriority){}
