package com.axionix.team_task_tracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TaskApiTest extends ApiTestBase {

    private String ownerToken;
    private String bobToken;
    private long bobId;
    private long carolId;
    private long projectId;

    @BeforeEach
    void setUp() throws Exception {
        registerUser("Owner", "owner@example.com");
        bobId = registerUser("Bob", "bob@example.com");
        carolId = registerUser("Carol", "carol@example.com");
        ownerToken = loginAs("owner@example.com");
        bobToken = loginAs("bob@example.com");
        projectId = createProject(ownerToken, "Alpha");
        addMember(ownerToken, projectId, "bob@example.com");   // Carol is NOT a member
    }

    @Test
    void assigneeMustBeProjectMember() throws Exception {
        postTask(ownerToken, projectId, """
                {"title":"Task","priority":"LOW","dueDate":"2099-12-31T18:00:00","assigneeId":%d}
                """.formatted(carolId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ASSIGNEE"));
    }

    @Test
    void memberCanBeAssigned() throws Exception {
        postTask(ownerToken, projectId, """
                {"title":"Task","priority":"LOW","dueDate":"2099-12-31T18:00:00","assigneeId":%d}
                """.formatted(bobId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assigneeId").value(bobId));
    }

    @Test
    void completedAtIsSetWhenDoneAndClearedWhenReopened() throws Exception {
        long taskId = createTask(ownerToken, projectId, "Write docs", "MEDIUM");

        patchTask(bobToken, taskId, "{\"status\":\"DONE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        patchTask(bobToken, taskId, "{\"status\":\"TODO\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.completedAt").isEmpty());
    }

    @Test
    void listCanBeFilteredByStatusAndPriority() throws Exception {
        createTask(ownerToken, projectId, "A", "HIGH");
        createTask(ownerToken, projectId, "B", "LOW");
        long c = createTask(ownerToken, projectId, "C", "HIGH");
        patchTask(ownerToken, c, "{\"status\":\"DONE\"}").andExpect(status().isOk());

        authGet(ownerToken, "/projects/" + projectId + "/tasks?status=TODO&priority=HIGH")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("A"));
    }

    @Test
    void pageSizeAboveMaximumIsRejected() throws Exception {
        authGet(ownerToken, "/projects/" + projectId + "/tasks?size=51")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PAGINATION"));
    }

    @Test
    void onlyOwnerOrCreatorCanDeleteTask() throws Exception {
        long taskId = createTask(ownerToken, projectId, "Task", "LOW");   // created by the owner

        authDelete(bobToken, "/tasks/" + taskId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("DELETE_NOT_ALLOWED"));
        authDelete(ownerToken, "/tasks/" + taskId)
                .andExpect(status().isNoContent());
    }

    @Test
    void summaryCountsTasksByStatusAndPriority() throws Exception {
        long first = createTask(ownerToken, projectId, "A", "HIGH");
        createTask(ownerToken, projectId, "B", "LOW");
        patchTask(ownerToken, first, "{\"status\":\"DONE\"}").andExpect(status().isOk());

        authGet(ownerToken, "/projects/" + projectId + "/summary")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.byStatus.DONE").value(1))
                .andExpect(jsonPath("$.byStatus.TODO").value(1))
                .andExpect(jsonPath("$.byStatus.IN_PROGRESS").value(0))
                .andExpect(jsonPath("$.byPriority.HIGH").value(1))
                .andExpect(jsonPath("$.byPriority.LOW").value(1));
    }
}