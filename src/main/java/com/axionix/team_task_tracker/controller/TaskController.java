package com.axionix.team_task_tracker.controller;

import com.axionix.team_task_tracker.dto.CreateTaskRequest;
import com.axionix.team_task_tracker.dto.TaskResponse;
import com.axionix.team_task_tracker.dto.UpdateTaskRequest;
import com.axionix.team_task_tracker.entity.User;
import com.axionix.team_task_tracker.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> create(@PathVariable Long projectID,
                                               @Valid @RequestBody CreateTaskRequest req,
                                               @AuthenticationPrincipal User user){
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(projectID,req,user));
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
}
