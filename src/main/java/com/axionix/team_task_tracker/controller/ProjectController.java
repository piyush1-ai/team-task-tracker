package com.axionix.team_task_tracker.controller;

import com.axionix.team_task_tracker.dto.AddMemberRequest;
import com.axionix.team_task_tracker.dto.CreateProjectRequest;
import com.axionix.team_task_tracker.dto.ProjectDetailResponse;
import com.axionix.team_task_tracker.dto.ProjectResponse;
import com.axionix.team_task_tracker.entity.User;
import com.axionix.team_task_tracker.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest req,
                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(req, user));
    }

    @GetMapping
    public List<ProjectResponse> list(@AuthenticationPrincipal User user) {
        return projectService.listForUser(user);
    }

    @GetMapping("/{projectId}")
    public ProjectDetailResponse get(@PathVariable Long projectId,
                                     @AuthenticationPrincipal User user) {
        return projectService.getById(projectId, user);
    }

    @PostMapping("/{projectId}/members")
    public ProjectDetailResponse addMember(@PathVariable Long projectId,
                                           @Valid @RequestBody AddMemberRequest req,
                                           @AuthenticationPrincipal User user) {
        return projectService.addMember(projectId, req, user);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId,
                                       @AuthenticationPrincipal User user) {
        projectService.delete(projectId, user);
        return ResponseEntity.noContent().build();
    }

}
