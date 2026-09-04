package com.modelmate.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modelmate.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String register(String email) throws Exception {
        String body = """
                {"firstName":"Test","lastName":"User","email":"%s","password":"password123"}
                """.formatted(email);
        String response = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("token").asText();
    }

    @Test
    void registerThenFetchMe() throws Exception {
        String token = register("alice@example.com");

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/me"));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        register("bob@example.com");
        String body = """
                {"firstName":"Bob","lastName":"Two","email":"bob@example.com","password":"password123"}
                """;
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void shortPasswordFailsValidation() throws Exception {
        String body = """
                {"firstName":"X","lastName":"Y","email":"short@example.com","password":"abc"}
                """;
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").isNotEmpty());
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        register("carol@example.com");
        String body = """
                {"email":"carol@example.com","password":"wrong-password"}
                """;
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSucceedsWithCorrectPassword() throws Exception {
        register("dave@example.com");
        String body = """
                {"email":"dave@example.com","password":"password123"}
                """;
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void adminRoutesRequireAdminRole() throws Exception {
        String userToken = register("eve@example.com");

        // authenticated USER -> 403 (blocked by role rule, before routing)
        mvc.perform(get("/api/v1/admin/anything").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // unauthenticated -> 401
        mvc.perform(get("/api/v1/admin/anything"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordResetFullFlow() throws Exception {
        register("frank@example.com");

        mvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"frank@example.com\"}"))
                .andExpect(status().isNoContent());

        String code = TestPasswordResetCodeCapture.LAST_CODE.get();
        assertThat(code).matches("\\d{6}");

        String verifyResponse = mvc.perform(post("/api/v1/auth/verify-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"frank@example.com\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String ticket = json.readTree(verifyResponse).get("resetTicket").asText();

        mvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resetTicket\":\"" + ticket + "\",\"newPassword\":\"brand-new-pass-1\"}"))
                .andExpect(status().isNoContent());

        // old password no longer works, new one does
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"frank@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"frank@example.com\",\"password\":\"brand-new-pass-1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void wrongResetCodeIsRejected() throws Exception {
        register("grace@example.com");
        mvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"grace@example.com\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/auth/verify-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"grace@example.com\",\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPasswordForUnknownEmailStillReturns204() throws Exception {
        mvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isNoContent());
    }
}
