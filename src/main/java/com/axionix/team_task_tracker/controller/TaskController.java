package com.axionix.team_task_tracker.controller;

import com.axionix.team_task_tracker.dto.*;
import com.axionix.team_task_tracker.entity.Priority;
import com.axionix.team_task_tracker.entity.Status;
import com.axionix.team_task_tracker.entity.User;
import com.axionix.team_task_tracker.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> create(@PathVariable Long projectId,
                                               @Valid @RequestBody CreateTaskRequest req,
                                               @AuthenticationPrincipal User user){
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(projectId,req,user));
    }
    @PatchMapping("/tasks/{taskId}")
    public TaskResponse update(@PathVariable Long taskId,
                               @Valid @RequestBody UpdateTaskRequest req,
                               @AuthenticationPrincipal User user){
        return taskService.update(taskId,req,user);
    }
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> delete(@PathVariable Long taskId,
                                       @AuthenticationPrincipal User user) {
        taskService.delete(taskId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/projects/{projectId}/tasks")
    public PageResponse<TaskResponse> list(
            @PathVariable("projectId") Long projectId,
            @RequestParam(name = "status", required = false) Status status,
            @RequestParam(name = "priority", required = false) Priority priority,
            @RequestParam(name = "assigneeId", required = false) Long assigneeId,
            @RequestParam(name = "dueFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueFrom,
            @RequestParam(name = "dueTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueTo,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "created_at") String sortBy,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            @AuthenticationPrincipal User user) {
        TaskFilter filter = new TaskFilter(status, priority, assigneeId, dueFrom, dueTo);
        return taskService.list(projectId, filter, page, size, sortBy, direction, user);
    }

    @GetMapping("/projects/{projectId}/summary")
    public SummaryResponse summary(@PathVariable Long projectId ,
                                   @AuthenticationPrincipal User user){
        return taskService.summary(projectId,user);
    }
}
