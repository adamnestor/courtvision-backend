package com.adamnestor.courtvision.test.integration;

import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("API Flow Integration Tests")
class ApiIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Dashboard should return stats with default parameters")
    void dashboardDefaultFlow() throws Exception {
        // Create test data
        String playerId = createTestPlayer();
        createTestGames(playerId, 15);  // Create enough games for L10 analysis

        mockMvc.perform(get("/api/dashboard/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].playerId").value(playerId))
                .andExpect(jsonPath("$.data[0].category").value("POINTS"))
                .andExpect(jsonPath("$.data[0].timePeriod").value("L10"))
                .andExpect(jsonPath("$.data[0].hitRate").isNumber())
                .andExpect(jsonPath("$.data[0].average").isNumber());
    }

    @Test
    @DisplayName("Dashboard should handle filtering parameters correctly")
    void dashboardFilteredFlow() throws Exception {
        // Create test data
        String playerId = createTestPlayer();
        createTestGames(playerId, 20);  // Create enough games for all periods

        mockMvc.perform(get("/api/dashboard/stats")
                        .param("timePeriod", "L5")
                        .param("category", "ASSISTS")
                        .param("threshold", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].timePeriod").value("L5"))
                .andExpect(jsonPath("$.data[0].category").value("ASSISTS"))
                .andExpect(jsonPath("$.data[0].threshold").value(5));
    }

    @Test
    @DisplayName("Player detail endpoint should return complete stats")
    void playerDetailFlow() throws Exception {
        // Create test data
        String playerId = createTestPlayer();
        createTestGames(playerId, 10);

        mockMvc.perform(get("/api/players/" + playerId + "/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.player.playerId").value(playerId))
                .andExpect(jsonPath("$.data.games").isArray())
                .andExpect(jsonPath("$.data.summary").exists());
    }

    @Test
    @DisplayName("Player detail should handle filtering parameters")
    void playerDetailFilteredFlow() throws Exception {
        // Create test data
        String playerId = createTestPlayer();
        createTestGames(playerId, 15);

        mockMvc.perform(get("/api/players/" + playerId + "/stats")
                        .param("timePeriod", "L5")
                        .param("category", "REBOUNDS")
                        .param("threshold", "8")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.category").value("REBOUNDS"))
                .andExpect(jsonPath("$.data.summary.timePeriod").value("L5"))
                .andExpect(jsonPath("$.data.summary.threshold").value(8));
    }

    @Test
    @DisplayName("API should handle invalid player ID gracefully")
    void invalidPlayerFlow() throws Exception {
        mockMvc.perform(get("/api/players/999/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("API should handle insufficient data gracefully")
    void insufficientDataFlow() throws Exception {
        // Create player with insufficient games for L10 analysis
        String playerId = createTestPlayer();
        createTestGames(playerId, 3);

        mockMvc.perform(get("/api/players/" + playerId + "/stats")
                        .param("timePeriod", "L10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.games").isArray())
                .andExpect(jsonPath("$.data.games.length()").value(3));
    }
}