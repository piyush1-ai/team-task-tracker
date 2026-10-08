package com.axionix.team_task_tracker.repository;

import com.axionix.team_task_tracker.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("select p from Project p join p.members m where m.id = :userId")
    List<Project> findByMemberId(@Param("userId") Long userId);

    boolean existsByIdAndMembers_Id(Long projectId , Long userId);
}
