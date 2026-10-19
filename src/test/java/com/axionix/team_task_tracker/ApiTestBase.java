package com.axionix.team_task_tracker;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class ApiTestBase {

    @Autowired
    protected MockMvc mockMvc;

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected long idOf(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    protected long registerUser(String name, String email) throws Exception {
        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","email":"%s","password":"password123"}
                                """.formatted(name, email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(body);
    }

    protected String loginAs(String email) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    protected long createProject(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/projects")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(body);
    }

    protected void addMember(String ownerToken, long projectId, String email) throws Exception {
        mockMvc.perform(post("/projects/" + projectId + "/members")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk());
    }

    protected ResultActions postTask(String token, long projectId, String json) throws Exception {
        return mockMvc.perform(post("/projects/" + projectId + "/tasks")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    protected long createTask(String token, long projectId, String title, String priority) throws Exception {
        String body = postTask(token, projectId, """
                {"title":"%s","priority":"%s","dueDate":"2099-12-31T18:00:00"}
                """.formatted(title, priority))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(body);
    }

    protected ResultActions patchTask(String token, long taskId, String json) throws Exception {
        return mockMvc.perform(patch("/tasks/" + taskId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    protected ResultActions authGet(String token, String url) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", bearer(token)));
    }

    protected ResultActions authDelete(String token, String url) throws Exception {
        return mockMvc.perform(delete(url).header("Authorization", bearer(token)));
    }
}