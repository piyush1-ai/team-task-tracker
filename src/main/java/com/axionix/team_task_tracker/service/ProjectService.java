package com.axionix.team_task_tracker.service;

import com.axionix.team_task_tracker.dto.*;
import com.axionix.team_task_tracker.entity.Project;
import com.axionix.team_task_tracker.entity.User;
import com.axionix.team_task_tracker.repository.ProjectRepository;
import com.axionix.team_task_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProjectResponse create(CreateProjectRequest req, User currentUser) {
        Project project = new Project();
        project.setName(req.name().trim());
        project.setDescription(req.description());
        project.setOwner(currentUser);
        project.getMembers().add(currentUser);
        return toResponse(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listForUser(User currentUser) {
        return projectRepository.findByMemberId(currentUser.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getById(Long projectId, User currentUser) {
        Project project = findProject(projectId);
        requireMember(projectId, currentUser);
        return toDetail(project);
    }

    @Transactional
    public ProjectDetailResponse addMember(Long projectId, AddMemberRequest req , User currentUser) {
        Project project = findProject(projectId);
        requireOwner(project, currentUser);

        User newMember = userRepository.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No registered user with that email"
                ));
        if (!project.getMembers().add(newMember)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member");
        }
        return toDetail(projectRepository.save(project));
    }

    @Transactional
    public void delete(Long projectId, User currentUser) {
        Project project = findProject(projectId);
        requireOwner(project, currentUser);
        projectRepository.delete(project);
    }

    // -------------helper functions ----------------
    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"
                ));

    }
    private void requireMember(Long projectId , User user){
        if (!projectRepository.existsByIdAndMembers_Id(projectId,user.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, " You are not a member of this project");
    }
    private void requireOwner(Project project , User user){
        if (!project.getOwner().getId().equals(user.getId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the owner can do this");
        }
    }
    private ProjectResponse toResponse(Project p){
        return new ProjectResponse(p.getId(),p.getName(),p.getDescription(),
                p.getOwner().getId(),p.getCreatedAt());
    }
    private ProjectDetailResponse toDetail(Project p){
        List<UserResponse> members = p.getMembers().stream()
                .map(u->new UserResponse(u.getId(),u.getName(),u.getEmail()))
                .toList();
        return new ProjectDetailResponse(p.getId(),p.getName(),p.getDescription(),p.getOwner().getId(),p.getCreatedAt(),members);
    }
}
