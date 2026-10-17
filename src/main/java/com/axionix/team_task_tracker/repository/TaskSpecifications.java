package com.axionix.team_task_tracker.repository;

import com.axionix.team_task_tracker.entity.Priority;
import com.axionix.team_task_tracker.entity.Status;
import com.axionix.team_task_tracker.entity.Task;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class TaskSpecifications {
    private TaskSpecifications(){}

    public static Specification<Task> inProject(Long projectId){
        return ((root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("project").get("id"),projectId
        ));
    }

    public static Specification<Task> hasStatus(Status status){
        return
                 ((root, query, cb) ->cb.equal(root.get("status"),status));
    }
    public static Specification<Task> hasPriority(Priority priority){
        return
                (root, query, cb) -> cb.equal(root.get("priority"),priority);
    }
    public static Specification<Task> hasAssignee(Long assigneeId){
        return
                (root, query, cb) -> cb.equal(root.get("assignee").get("id"),assigneeId);
    }
    public static Specification<Task> dueFrom(LocalDateTime from){
        return
                (root, query, cb) -> cb.greaterThanOrEqualTo(root.<LocalDateTime>get("dueDate"),from);
    }
    public static Specification<Task> dueTo(LocalDateTime to) {
        return
                 (root, query, cb) -> cb.lessThanOrEqualTo(root.<LocalDateTime>get("dueDate"), to);
    }
}
