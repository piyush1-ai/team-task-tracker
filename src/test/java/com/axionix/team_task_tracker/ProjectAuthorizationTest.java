package com.axionix.team_task_tracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectAuthorizationTest extends ApiTestBase {

    private String ownerToken;
    private String bobToken;
    private long projectId;

    @BeforeEach
    void setUp() throws Exception {
        registerUser("Owner", "owner@example.com");
        registerUser("Bob", "bob@example.com");
        ownerToken = loginAs("owner@example.com");
        bobToken = loginAs("bob@example.com");
        projectId = createProject(ownerToken, "Alpha");
    }

    @Test
    void nonMemberCannotViewProject() throws Exception {
        authGet(bobToken, "/projects/" + projectId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_A_MEMBER"));
    }

    @Test
    void memberCanViewProjectAfterBeingAdded() throws Exception {
        addMember(ownerToken, projectId, "bob@example.com");

        authGet(bobToken, "/projects/" + projectId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(2));
    }

    @Test
    void memberCannotAddOtherMembers() throws Exception {
        addMember(ownerToken, projectId, "bob@example.com");

        mockMvc.perform(post("/projects/" + projectId + "/members")
                        .header("Authorization", bearer(bobToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bob@example.com\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_PROJECT_OWNER"));
    }

    @Test
    void onlyOwnerCanDeleteProject() throws Exception {
        addMember(ownerToken, projectId, "bob@example.com");

        authDelete(bobToken, "/projects/" + projectId)
                .andExpect(status().isForbidden());
        authDelete(ownerToken, "/projects/" + projectId)
                .andExpect(status().isNoContent());
        authGet(ownerToken, "/projects/" + projectId)
                .andExpect(status().isNotFound());
    }
}