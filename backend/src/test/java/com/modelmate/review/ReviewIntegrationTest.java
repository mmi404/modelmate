package com.modelmate.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modelmate.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String register(String email) throws Exception {
        String body = """
                {"firstName":"R","lastName":"T","email":"%s","password":"password123"}
                """.formatted(email);
        String res = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("token").asText();
    }

    private long modelId(String slug) throws Exception {
        String res = mvc.perform(get("/api/v1/models/" + slug))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("id").asLong();
    }

    private static final String FULL_REVIEW = """
            {"type":"REVIEW","content":"Solid model overall.",
             "ratings":{"accuracy":5,"speed":4,"cost":3,"easeOfUse":4,"reliability":5}}
            """;

    @Test
    void reviewRequiresAllFiveRatings() throws Exception {
        String token = register("rev1@example.com");
        long id = modelId("gpt-4");
        mvc.perform(post("/api/v1/models/" + id + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"REVIEW\",\"content\":\"no ratings\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postReviewComputesOverallAndBlocksDuplicates() throws Exception {
        String token = register("rev2@example.com");
        long id = modelId("gpt-4");

        mvc.perform(post("/api/v1/models/" + id + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(FULL_REVIEW))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.overallRating").value(4.20))
                .andExpect(jsonPath("$.type").value("REVIEW"));

        mvc.perform(post("/api/v1/models/" + id + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(FULL_REVIEW))
                .andExpect(status().isConflict());
    }

    @Test
    void reviewFlowsIntoModelAggregateAndLeaderboard() throws Exception {
        String token = register("rev3@example.com");
        long id = modelId("gpt-4");

        mvc.perform(post("/api/v1/models/" + id + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(FULL_REVIEW))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/models/gpt-4"))
                .andExpect(jsonPath("$.ratings.reviewCount").value(1))
                .andExpect(jsonPath("$.ratings.overall").value(4.20));

        mvc.perform(get("/api/v1/models/" + id + "/reviews"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].myVote").doesNotExist());

        mvc.perform(get("/api/v1/leaderboard").param("minReviews", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("gpt-4"))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].topThree").value(true))
                .andExpect(jsonPath("$[0].reviewCount").value(1));
    }

    @Test
    void problemReportsHaveSeverityAndCountSeparately() throws Exception {
        String token = register("rev4@example.com");
        long id = modelId("bert");

        mvc.perform(post("/api/v1/models/" + id + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"PROBLEM\",\"title\":\"Tokenizer bug\",\"content\":\"breaks on emoji\",\"severity\":\"HIGH\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.ratings").doesNotExist());

        mvc.perform(get("/api/v1/models/" + id + "/problems"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/models/bert"))
                .andExpect(jsonPath("$.problemCount").value(1))
                .andExpect(jsonPath("$.ratings.reviewCount").value(0));
    }

    @Test
    void onlyAuthorCanEditOrDelete() throws Exception {
        String author = register("owner@example.com");
        String other = register("intruder@example.com");
        long id = modelId("llama");

        String created = mvc.perform(post("/api/v1/models/" + id + "/reviews")
                        .header("Authorization", "Bearer " + author)
                        .contentType(MediaType.APPLICATION_JSON).content(FULL_REVIEW))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long reviewId = json.readTree(created).get("id").asLong();

        String edit = """
                {"content":"Edited take.","ratings":{"accuracy":3,"speed":3,"cost":3,"easeOfUse":3,"reliability":3}}
                """;
        mvc.perform(put("/api/v1/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + other)
                        .contentType(MediaType.APPLICATION_JSON).content(edit))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + author)
                        .contentType(MediaType.APPLICATION_JSON).content(edit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallRating").value(3.00));

        mvc.perform(delete("/api/v1/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + author))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/models/" + id + "/reviews"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void postingReviewRequiresAuth() throws Exception {
        long id = modelId("gpt-4");
        mvc.perform(post("/api/v1/models/" + id + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON).content(FULL_REVIEW))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recentFeedIsPublicAndReturnsNewestFirst() throws Exception {
        String token = register("recent@example.com");
        long gpt = modelId("gpt-4");
        long bert = modelId("bert");

        mvc.perform(post("/api/v1/models/" + gpt + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(FULL_REVIEW))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/models/" + bert + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"PROBLEM\",\"content\":\"Edge case with long inputs.\",\"severity\":\"LOW\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/reviews/recent?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].modelSlug").value("bert"))
                .andExpect(jsonPath("$[0].type").value("PROBLEM"))
                .andExpect(jsonPath("$[1].modelSlug").value("gpt-4"))
                .andExpect(jsonPath("$[1].snippet").value("Solid model overall."));
    }
}
