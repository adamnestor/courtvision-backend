package com.adamnestor.courtvision.integration;

import com.adamnestor.courtvision.dto.response.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.hamcrest.Matchers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.matchesPattern;

@SpringBootTest
@AutoConfigureMockMvc
public class ResponseFormatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void playerStatsResponse_ShouldMatchRequiredFormat() throws Exception {
        mockMvc.perform(get("/api/players/1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.playerId").exists())
                .andExpect(jsonPath("$.data.hitRate").isNumber())
                .andExpect(jsonPath("$.data.confidenceScore").isNumber())
                .andExpect(jsonPath("$.data.isHighConfidence").isBoolean());
    }

    @Test
    void picksResponse_ShouldMatchRequiredFormat() throws Exception {
        mockMvc.perform(get("/api/picks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].result").value(Matchers.anyOf(
                    Matchers.is("WIN"),
                    Matchers.is("LOSS"),
                    Matchers.nullValue()
                )))
                .andExpect(jsonPath("$.data[0].createdAt").value(
                    matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")
                ));
    }

    @Test
    void dashboardResponse_ShouldMatchRequiredFormat() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].lastGames").isArray())
                .andExpect(jsonPath("$.data[0].hitRate").isNumber())
                .andExpect(jsonPath("$.data[0].isHighConfidence").isBoolean());
    }
} 