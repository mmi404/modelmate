package com.modelmate.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modelmate.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminProfileIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String register(String email) throws Exception {
        String body = """
                {"firstName":"A","lastName":"P","email":"%s","password":"password123"}
                """.formatted(email);
        return json.readTree(mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("token").asText();
    }

    private String login(String email) throws Exception {
        String body = """
                {"email":"%s","password":"password123"}
                """.formatted(email);
        return json.readTree(mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("token").asText();
    }

    private long submitModel(String token, String name) throws Exception {
        String body = """
                {"name":"%s","categoryId":1,"websiteUrl":"example.com","description":"d"}
                """.formatted(name);
        return json.readTree(mvc.perform(post("/api/v1/models")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString())
                .get("submissionId").asLong();
    }

    private long modelId(String slug) throws Exception {
        return json.readTree(mvc.perform(get("/api/v1/models/" + slug))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("id").asLong();
    }

    @Test
    void adminEndpointsRejectNonAdmins() throws Exception {
        String userToken = register("plainuser@example.com");
        mvc.perform(get("/api/v1/admin/models/pending").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approveMakesModelPublicAndSecondApproveConflicts() throws Exception {
        String user = register("submitter@example.com");
        register("boss@example.com");
        promoteToAdmin("boss@example.com");
        String admin = login("boss@example.com");

        long submissionId = submitModel(user, "Claude Sonnet");

        mvc.perform(get("/api/v1/admin/models/pending").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].submitterName").value("A P"));

        mvc.perform(post("/api/v1/admin/models/" + submissionId + "/approve")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(get("/api/v1/models")).andExpect(jsonPath("$.totalElements").value(13));
        mvc.perform(get("/api/v1/admin/models/pending").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(post("/api/v1/admin/models/" + submissionId + "/approve")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectKeepsModelOutOfPublicListings() throws Exception {
        String user = register("submitter2@example.com");
        register("boss2@example.com");
        promoteToAdmin("boss2@example.com");
        String admin = login("boss2@example.com");

        long submissionId = submitModel(user, "Rejected Model");
        mvc.perform(post("/api/v1/admin/models/" + submissionId + "/reject")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"duplicate\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mvc.perform(get("/api/v1/models")).andExpect(jsonPath("$.totalElements").value(12));
    }

    @Test
    void adminCanHideAndUnhideReviews() throws Exception {
        String user = register("reviewer@example.com");
        register("boss3@example.com");
        promoteToAdmin("boss3@example.com");
        String admin = login("boss3@example.com");
        long gpt4 = modelId("gpt-4");

        long reviewId = json.readTree(mvc.perform(post("/api/v1/models/" + gpt4 + "/reviews")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"REVIEW","content":"hidden soon",
                                 "ratings":{"accuracy":4,"speed":4,"cost":4,"easeOfUse":4,"reliability":4}}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .get("id").asLong();

        mvc.perform(patch("/api/v1/admin/reviews/" + reviewId + "/hidden")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"hidden\":true}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/models/" + gpt4 + "/reviews"))
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(patch("/api/v1/admin/reviews/" + reviewId + "/hidden")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"hidden\":false}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/models/" + gpt4 + "/reviews"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void adminStatsReflectSeedAndActivity() throws Exception {
        register("boss4@example.com");
        promoteToAdmin("boss4@example.com");
        String admin = login("boss4@example.com");

        mvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvedModels").value(12))
                .andExpect(jsonPath("$.pendingModels").value(0))
                .andExpect(jsonPath("$.totalReviews").value(0));
    }

    @Test
    void publicProfileHidesEmailAndListsContributions() throws Exception {
        String token = register("contributor@example.com");
        long userId = json.readTree(mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        long gpt4 = modelId("gpt-4");

        mvc.perform(post("/api/v1/models/" + gpt4 + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"REVIEW","content":"my review",
                                 "ratings":{"accuracy":5,"speed":5,"cost":5,"easeOfUse":5,"reliability":5}}"""))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/discussions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hello\",\"content\":\"world\",\"tags\":[\"intro\"]}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.firstName").value("A"));

        mvc.perform(get("/api/v1/users/" + userId + "/contributions"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].type").exists());

        mvc.perform(put("/api/v1/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"New\",\"lastName\":\"Name\",\"bio\":\"AI enthusiast\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("AI enthusiast"));

        mvc.perform(get("/api/v1/me/contributions").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}
