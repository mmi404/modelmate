package com.modelmate.community;

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

class CommunityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String register(String email) throws Exception {
        String body = """
                {"firstName":"C","lastName":"M","email":"%s","password":"password123"}
                """.formatted(email);
        String res = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("token").asText();
    }

    private long createDiscussion(String token) throws Exception {
        String body = """
                {"title":"How does RAG scale?","content":"Curious about vector DB costs.","tags":["RAG","Cost"]}
                """;
        String res = mvc.perform(post("/api/v1/discussions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tags", org.hamcrest.Matchers.containsInAnyOrder("rag", "cost")))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("id").asLong();
    }

    @Test
    void createRequiresAuth() throws Exception {
        mvc.perform(post("/api/v1/discussions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"content\":\"y\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createListFilterAndStats() throws Exception {
        String token = register("comm1@example.com");
        long id = createDiscussion(token);

        mvc.perform(get("/api/v1/discussions"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value((int) id));

        mvc.perform(get("/api/v1/discussions").param("tags", "rag"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/discussions").param("tags", "unused-tag"))
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(get("/api/v1/discussions/tags"))
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(get("/api/v1/discussions/stats"))
                .andExpect(jsonPath("$.totalDiscussions").value(1))
                .andExpect(jsonPath("$.activeMembers").value(1))
                .andExpect(jsonPath("$.totalReplies").value(0));
    }

    @Test
    void repliesThreadOneLevelAndBumpCount() throws Exception {
        String token = register("comm2@example.com");
        long id = createDiscussion(token);

        String r1 = mvc.perform(post("/api/v1/discussions/" + id + "/replies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"top-level reply\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentReplyId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long parentId = json.readTree(r1).get("id").asLong();

        mvc.perform(post("/api/v1/discussions/" + id + "/replies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"nested\",\"parentReplyId\":" + parentId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentReplyId").value((int) parentId));

        mvc.perform(get("/api/v1/discussions/" + id + "/replies"))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/v1/discussions/" + id))
                .andExpect(jsonPath("$.replyCount").value(2));
    }

    @Test
    void votingOnDiscussionAdjustsCountsAndMyVote() throws Exception {
        String author = register("comm3@example.com");
        String voter = register("voter3@example.com");
        long id = createDiscussion(author);

        mvc.perform(put("/api/v1/votes")
                        .header("Authorization", "Bearer " + voter)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"DISCUSSION\",\"targetId\":" + id + ",\"value\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upvoteCount").value(1))
                .andExpect(jsonPath("$.myVote").value(1));

        // switch to downvote
        mvc.perform(put("/api/v1/votes")
                        .header("Authorization", "Bearer " + voter)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"DISCUSSION\",\"targetId\":" + id + ",\"value\":-1}"))
                .andExpect(jsonPath("$.upvoteCount").value(0))
                .andExpect(jsonPath("$.downvoteCount").value(1));

        // caller sees their vote reflected on the discussion
        mvc.perform(get("/api/v1/discussions/" + id).header("Authorization", "Bearer " + voter))
                .andExpect(jsonPath("$.myVote").value(-1));

        // remove
        mvc.perform(delete("/api/v1/votes")
                        .header("Authorization", "Bearer " + voter)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"DISCUSSION\",\"targetId\":" + id + "}"))
                .andExpect(jsonPath("$.upvoteCount").value(0))
                .andExpect(jsonPath("$.downvoteCount").value(0));
    }

    @Test
    void voteOnMissingTargetIs404() throws Exception {
        String token = register("comm4@example.com");
        mvc.perform(put("/api/v1/votes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"REVIEW\",\"targetId\":999999,\"value\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void votingRequiresAuth() throws Exception {
        mvc.perform(put("/api/v1/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"DISCUSSION\",\"targetId\":1,\"value\":1}"))
                .andExpect(status().isUnauthorized());
    }
}
