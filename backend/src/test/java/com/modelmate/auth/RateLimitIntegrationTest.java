package com.modelmate.auth;

import com.modelmate.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RateLimitIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void sixthLoginAttemptFromSameIpIsRateLimited() throws Exception {
        String body = "{\"email\":\"ratelimit@example.com\",\"password\":\"whatever12\"}";

        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}
