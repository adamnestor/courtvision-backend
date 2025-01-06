package com.adamnestor.courtvision.test.web;

import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import com.adamnestor.courtvision.dto.player.GameMetrics;
import com.adamnestor.courtvision.dto.player.GamePerformance;
import com.adamnestor.courtvision.dto.player.PlayerDetailStats;
import com.adamnestor.courtvision.dto.player.PlayerInfo;
import com.adamnestor.courtvision.dto.stats.StatsSummary;
import com.adamnestor.courtvision.security.jwt.JwtAuthenticationFilter;
import com.adamnestor.courtvision.security.jwt.JwtTokenUtil;
import com.adamnestor.courtvision.service.HitRateCalculationService;
import com.adamnestor.courtvision.test.config.TestSecurityConfig;
import com.adamnestor.courtvision.web.PlayerController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlayerController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class PlayerControllerTest {

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HitRateCalculationService statsService;

    @Test
    @WithMockUser
    void getPlayerStats_WithValidId_ReturnsPlayerStats() throws Exception {
        PlayerDetailStats playerStats = createPlayerDetailStats();
        when(statsService.getPlayerDetailStats(eq(1L), any(), any(), any()))
                .thenReturn(playerStats);

        mockMvc.perform(get("/api/players/1/stats")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.player.firstName").value("Test"))
                .andExpect(jsonPath("$.data.player.lastName").value("Player"))
                .andExpect(jsonPath("$.data.games").isArray())
                .andExpect(jsonPath("$.data.summary.hitRate").value(80.0));
    }

    @Test
    @WithMockUser
    void getPlayerStats_WithCustomParameters_ReturnsFilteredStats() throws Exception {
        PlayerDetailStats playerStats = createPlayerDetailStats();
        when(statsService.getPlayerDetailStats(eq(1L), any(), any(), any()))
                .thenReturn(playerStats);

        mockMvc.perform(get("/api/players/1/stats")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("timePeriod", "L5")
                        .param("category", "ASSISTS")
                        .param("threshold", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.games").exists());
    }

    @Test
    @WithMockUser
    void getPlayerStats_WithNonExistentId_Returns404() throws Exception {
        when(statsService.getPlayerDetailStats(eq(999L), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Player not found"));

        mockMvc.perform(get("/api/players/999/stats")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Player not found"));
    }

    @Test
    @WithMockUser
    void getPlayerStats_WithInvalidParameters_Returns400() throws Exception {
        mockMvc.perform(get("/api/players/abc/stats")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    private PlayerDetailStats createPlayerDetailStats() {
        PlayerInfo playerInfo = new PlayerInfo(1L, "Test", "Player", "DEN", "F");
        List<GamePerformance> games = List.of(
                new GamePerformance(1L, LocalDate.now(), "OPP", true, 22, 5, 8, "32:00", "100-95", true,5)
        );
        StatsSummary summary = new StatsSummary(
                StatCategory.POINTS, 20, TimePeriod.L10,
                new BigDecimal("80.00"), new BigDecimal("22.50"), 8, 2
        );

        return new PlayerDetailStats(
                playerInfo,
                games,
                summary,
                20,
                new GameMetrics(
                        30,
                        10,
                        22.5,
                        10,
                        8
                )
        );
    }
}