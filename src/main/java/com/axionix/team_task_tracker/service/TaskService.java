package com.axionix.team_task_tracker.service;

import com.axionix.team_task_tracker.dto.*;
import com.axionix.team_task_tracker.entity.*;
import com.axionix.team_task_tracker.repository.ProjectRepository;
import com.axionix.team_task_tracker.repository.TaskRepository;
import com.axionix.team_task_tracker.repository.TaskSpecifications;
import com.axionix.team_task_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private static final int MAX_PAGE_SIZE = 50;
    private static final Map<String, String> SORT_FIELDS =
            Map.of("created_at", "createdAt", "due_date", "dueDate");

    @Transactional
    public TaskResponse create(Long projectId, CreateTaskRequest req, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"
                ));
        requireMember(projectId, currentUser);
        validateDueDate(req.dueDate());

        Task task = new Task();
        task.setProject(project);
        task.setTitle(req.title().trim());
        task.setDescription(req.description());
        task.setPriority(req.priority());
        task.setStatus(Status.TODO);
        task.setDueDate(req.dueDate());
        task.setCreatedBy(currentUser);
        task.setAssignee(resolveAssignee(projectId, req.assigneeId()));
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long TaskId, UpdateTaskRequest req, User currentUser) {
        Task task = findTask(TaskId);
        Long projectId = task.getProject().getId();
        requireMember(projectId, currentUser);

        if (req.title() != null) {
            if (req.title().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title cannot be blank");
            }
            task.setTitle(req.title().trim());
        }
        if (req.description() != null) task.setDescription(req.description());
        if (req.priority() != null) task.setPriority(req.priority());
        if (req.dueDate() != null) {
            validateDueDate(req.dueDate());
            task.setDueDate(req.dueDate());
        }
        if (req.assigneeId() != null) {
            task.setAssignee(resolveAssignee(projectId, req.assigneeId()));
        }
        if (req.status() != null) applyStatus(task, req.status());
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long taskId, User currentUser) {
        Task task = findTask(taskId);
        boolean isOwner = task.getProject().getOwner().getId().equals(currentUser.getId());
        boolean isCreator = task.getCreatedBy().getId().equals(currentUser.getId());

        if (!isCreator && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the project owner or the task creator can delete the task");
        }
        taskRepository.delete(task);
    }

    //    ----- helpers---
    private void applyStatus(Task task, Status newStatus) {
        Status old = task.getStatus();
        if (old == newStatus) return;
        task.setStatus(newStatus);
        if (newStatus == Status.DONE) {
            task.setCompletedAt(LocalDateTime.now());
        } else if (old == Status.DONE) {
            task.setCompletedAt(null);
        }
    }

    private User resolveAssignee(Long projectId, Long assigneeId) {
        if (assigneeId == null) return null;
        if (!projectRepository.existsByIdAndMembers_Id(projectId, assigneeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Assignee must be a member of the project");
        }
        return userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assingee not found"));
    }

    private void validateDueDate(LocalDateTime dueDate) {
        if (dueDate.isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Due date cannot be in the past");
        }
    }

    private Task findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND
                        , " Task Not Found"));
    }

    private void requireMember(Long projectId, User user) {
        if (!projectRepository.existsByIdAndMembers_Id(projectId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not member of this project");
        }
    }

    private TaskResponse toResponse(Task t) {
        return new TaskResponse(t.getId(), t.getProject().getId(), t.getTitle()
                , t.getDescription(), t.getPriority(), t.getStatus(), t.getDueDate(),
                t.getAssignee() == null ? null : t.getAssignee().getId(),
                t.getCreatedBy().getId(), t.getCreatedAt(), t.getUpdatedAt(), t.getCompletedAt());
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(Long projectId, TaskFilter filter, int page, int size,
                                           String sortBy, String direction, User currentUser) {
        requireProjectAccess(projectId, currentUser);

        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be >= 0 and size must be between 1 and " + MAX_PAGE_SIZE);
        }
        String field = SORT_FIELDS.get(sortBy);
        if (field == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "sortBy must be created_ar or due_date");
        }
        Sort.Direction dir;
        try {
            dir = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "direction must be asc or desc");
        }
        if (filter.dueFrom() != null && filter.dueTo() != null
                && filter.dueFrom().isAfter(filter.dueTo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dueFrom must not be after dueTo");
        }
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(dir, field).and(Sort.by(Sort.Direction.ASC, "id")));

        Specification<Task> spec = TaskSpecifications.inProject(projectId);
        if (filter.status() != null) {
            spec = spec.and(TaskSpecifications.hasStatus(filter.status()));
        }
        if (filter.priority() != null) {
            spec = spec.and(TaskSpecifications.hasPriority(filter.priority()));
        }
        if (filter.assigneeId() != null) {
            spec = spec.and(TaskSpecifications.hasAssignee(filter.assigneeId()));
        }
        if (filter.dueFrom() != null) {
            spec = spec.and(TaskSpecifications.dueFrom(filter.dueFrom()));
        }
        if (filter.dueTo() != null) {
            spec = spec.and(TaskSpecifications.dueTo(filter.dueTo()));
        }
        Page<Task> result = taskRepository.findAll(spec, pageable);
        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }


    @Transactional(readOnly = true)
    public SummaryResponse summary(Long projectId, User currentUser) {
        requireProjectAccess(projectId, currentUser);

        Map<Status, Long> byStatus = new EnumMap<>(Status.class);
        for (Status s : Status.values()) {
            byStatus.put(s, taskRepository.countByProject_IdAndStatus(projectId, s));
        }

        Map<Priority, Long> byPriority = new EnumMap<>(Priority.class);
        for (Priority p : Priority.values()) {
            byPriority.put(p, taskRepository.countByProject_IdAndPriority(projectId, p));
        }

        return new SummaryResponse(taskRepository.countByProject_Id(projectId), byStatus, byPriority);
    }
    private void requireProjectAccess(Long projectId, User user) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        requireMember(projectId, user);
    }
}
