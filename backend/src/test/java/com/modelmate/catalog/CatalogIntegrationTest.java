package com.modelmate.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modelmate.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String userToken() throws Exception {
        String body = """
                {"firstName":"Cat","lastName":"Tester","email":"catalog-%d@example.com","password":"password123"}
                """.formatted(System.nanoTime());
        String response = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("token").asText();
    }

    // ----- categories --------------------------------------------------

    @Test
    void listsAllCategoriesWithCounts() throws Exception {
        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[?(@.slug=='computer-vision')].modelCount").value(org.hamcrest.Matchers.contains(3)));
    }

    @Test
    void getsOneCategoryWithApplicationsList() throws Exception {
        mvc.perform(get("/api/v1/categories/natural-language-processing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Natural Language Processing"))
                .andExpect(jsonPath("$.applications").isArray())
                .andExpect(jsonPath("$.applications[0]").value("Chatbots"));
    }

    @Test
    void unknownCategoryIs404() throws Exception {
        mvc.perform(get("/api/v1/categories/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsModelsInCategory() throws Exception {
        mvc.perform(get("/api/v1/categories/computer-vision/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].category.slug").value("computer-vision"));
    }

    // ----- models ----------------------------------------------------

    @Test
    void listsApprovedModels() throws Exception {
        mvc.perform(get("/api/v1/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(12));
    }

    @Test
    void filtersByCategoryAndQuery() throws Exception {
        mvc.perform(get("/api/v1/models").param("category", "natural-language-processing"))
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/v1/models").param("q", "gpt"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void sortByRatingDoesNotError() throws Exception {
        mvc.perform(get("/api/v1/models").param("sort", "rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(12));
    }

    @Test
    void trendingAndNamesAndSearch() throws Exception {
        mvc.perform(get("/api/v1/models/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12));
        mvc.perform(get("/api/v1/models/names"))
                .andExpect(jsonPath("$.length()").value(12));
        mvc.perform(get("/api/v1/models/search").param("q", "bert"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("bert"));
    }

    @Test
    void modelDetailAndCompare() throws Exception {
        mvc.perform(get("/api/v1/models/gpt-4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("gpt-4"))
                .andExpect(jsonPath("$.category.slug").value("natural-language-processing"))
                .andExpect(jsonPath("$.ratings.reviewCount").value(0))
                .andExpect(jsonPath("$.problemCount").value(0));

        mvc.perform(get("/api/v1/models/compare").param("slugs", "gpt-4,bert"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(get("/api/v1/models/compare").param("slugs", "gpt-4"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownModelIs404() throws Exception {
        mvc.perform(get("/api/v1/models/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void submitModelRequiresAuthAndStaysPending() throws Exception {
        String body = """
                {"name":"My New Model","categoryId":1,"websiteUrl":"example.com/model","description":"x"}
                """;

        mvc.perform(post("/api/v1/models").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/models")
                        .header("Authorization", "Bearer " + userToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.submissionId").isNumber());

        // still only the 12 approved seed models are listed
        mvc.perform(get("/api/v1/models"))
                .andExpect(jsonPath("$.totalElements").value(12));
    }
}
