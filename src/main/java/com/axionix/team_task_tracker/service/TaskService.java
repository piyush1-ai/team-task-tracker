package com.axionix.team_task_tracker.service;

import com.axionix.team_task_tracker.dto.CreateTaskRequest;
import com.axionix.team_task_tracker.dto.TaskResponse;
import com.axionix.team_task_tracker.dto.UpdateTaskRequest;
import com.axionix.team_task_tracker.entity.Project;
import com.axionix.team_task_tracker.entity.Status;
import com.axionix.team_task_tracker.entity.Task;
import com.axionix.team_task_tracker.entity.User;
import com.axionix.team_task_tracker.repository.ProjectRepository;
import com.axionix.team_task_tracker.repository.TaskRepository;
import com.axionix.team_task_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public TaskResponse create(Long projectId , CreateTaskRequest req , User currentUser){
        Project project = projectRepository.findById(projectId)
                .orElseThrow(()->new ResponseStatusException(
                        HttpStatus.NOT_FOUND , "Project not found"
                ));
        requireMember(projectId,currentUser);
        validateDueDate(req.dueDate());

        Task task = new Task();
        task.setProject(project);
        task.setTitle(req.title().trim());
        task.setDescription(req.description());
        task.setPriority(req.priority());
        task.setStatus(Status.TODO);
        task.setDueDate(req.dueDate());
        task.setAssignee(resolveAssignee(projectId,req.assigneeId()));
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long TaskId , UpdateTaskRequest req , User currentUser){
        Task task = findTask(TaskId);
        Long projectId = task.getProject().getId();
        requireMember(projectId,currentUser);

        if (req.title() != null){
            if (req.title().isBlank()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Title cannot be blank");
            }
            task.setTitle(req.title().trim());
        }
        if (req.description() != null) task.setDescription(req.description());
        if (req.priority() != null) task.setPriority(req.priority());
        if (req.dueDate() != null){
            validateDueDate(req.dueDate());
            task.setDueDate(req.dueDate());
        }
        if (req.assigneeId() != null){
            task.setAssignee(resolveAssignee(projectId,req.assigneeId()));
        }
        if (req.status() != null) applyStatus(task , req.status());
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long taskId , User currentUser){
        Task task = findTask(taskId);
        boolean isOwner = task.getProject().getOwner().getId().equals(currentUser.getId());
        boolean isCreator = task.getCreatedBy().getId().equals(currentUser.getId());

        if (!isCreator && !isOwner){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the project owner or the task creator can delete the task");
        }
        taskRepository.delete(task);
    }

//    ----- helpers---
    private void applyStatus(Task task, Status newStatus){
        Status old = task.getStatus();
        if (old == newStatus) return;
        task.setStatus(newStatus);
        if (newStatus == Status.DONE){
            task.setCompletedAt(LocalDateTime.now());
        } else if (old == Status.DONE) {
            task.setCompletedAt(null);
        }
    }

    private User resolveAssignee(Long projectId , Long assigneeId){
        if (assigneeId == null) return null;
        if (!projectRepository.existsByIdAndMembers_Id(projectId,assigneeId)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Assignee must be a member of the project");
        }
        return userRepository.findById(assigneeId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Assingee not found"));
    }
    private void validateDueDate(LocalDateTime dueDate){
        if (dueDate.isBefore(LocalDateTime.now())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Due date cannot be in the past");
        }
    }
    private Task findTask(Long taskId){
        return taskRepository.findById(taskId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND
                ," Task Not Found"));
    }
    private void requireMember(Long projectId , User user){
        if (!projectRepository.existsByIdAndMembers_Id(projectId,user.getId())){
            throw  new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not member of this project");
        }
    }

    private TaskResponse toResponse(Task t){
        return new TaskResponse(t.getId(),t.getProject().getId(),t.getTitle()
        ,t.getDescription(),t.getPriority(),t.getStatus(),t.getDueDate(),
        t.getAssignee() == null ? null :t.getAssignee().getId(),
               t.getCreatedBy().getId(),t.getCreatedAt(), t.getUpdatedAt(),t.getCompletedAt());
    }
}
