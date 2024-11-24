package com.adamnestor.courtvision.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenAccessingProtectedEndpoint_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/picks")
                        .with(anonymous())
                        .header("Accept", "application/json"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenAccessingPublicEndpoint_thenOk() throws Exception {
        mockMvc.perform(get("/api/public/stats")
                        .with(anonymous())
                        .header("Accept", "application/json"))
                .andDo(print())
                .andExpect(status().isOk());
    }
}