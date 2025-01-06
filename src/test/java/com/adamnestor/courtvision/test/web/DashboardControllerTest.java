package com.adamnestor.courtvision.test.web;

import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import com.adamnestor.courtvision.dto.dashboard.DashboardStatsRow;
import com.adamnestor.courtvision.security.jwt.JwtAuthenticationFilter;
import com.adamnestor.courtvision.security.jwt.JwtTokenUtil;
import com.adamnestor.courtvision.service.HitRateCalculationService;
import com.adamnestor.courtvision.test.config.TestSecurityConfig;
import com.adamnestor.courtvision.web.DashboardController;  // Add this import
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@Import({TestSecurityConfig.class})
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

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
    void getDashboardStats_WithDefaultParameters_ReturnsStats() throws Exception {
        DashboardStatsRow statsRow = new DashboardStatsRow(
                1L,                // playerId
                "Test Player",     // playerName
                "DEN",            // team
                "vs OPP",         // opponent
                "Points 20+",     // statLine
                new BigDecimal("80.00"),   // hitRate
                new BigDecimal("22.50")    // average
        );
        when(statsService.getDashboardStats(any(), any(), any(), any(), any()))
                .thenReturn(List.of(statsRow));

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].playerName").value("Test Player"))
                .andExpect(jsonPath("$.data[0].hitRate").value("80.0"))
                .andExpect(jsonPath("$.data[0].average").value("22.5"));
    }

    @Test
    @WithMockUser
    void getDashboardStats_WithCustomParameters_ReturnsFilteredStats() throws Exception {
        DashboardStatsRow statsRow = new DashboardStatsRow(
                1L,                // playerId
                "Test Player",     // playerName
                "DEN",            // team
                "vs OPP",         // opponent
                "Points 20+",     // statLine
                new BigDecimal("80.00"),   // hitRate
                new BigDecimal("22.50")    // average
        );
        when(statsService.getDashboardStats(
                eq(TimePeriod.L5),
                eq(StatCategory.ASSISTS),
                eq(4),
                eq("average"),
                eq("asc")))
                .thenReturn(List.of(statsRow));

        MvcResult result = mockMvc.perform(get("/api/dashboard/stats")
                        .param("timePeriod", "L5")
                        .param("category", "ASSISTS")
                        .param("threshold", "4")
                        .param("sortBy", "average")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andDo(print()) // Add this to see the full response
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("Response content: " + content);

        // Then add the assertions
        mockMvc.perform(get("/api/dashboard/stats")
                        .param("timePeriod", "L5")
                        .param("category", "ASSISTS")
                        .param("threshold", "4")
                        .param("sortBy", "average")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].category").value("ASSISTS"))
                .andExpect(jsonPath("$.data[0].threshold").value(4))
                .andExpect(jsonPath("$.data[0].timePeriod").value("L5"));
    }

    @Test
    @WithMockUser
    void getDashboardStats_WithEmptyResults_ReturnsEmptyList() throws Exception {
        when(statsService.getDashboardStats(any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @WithMockUser
    void getDashboardStats_WithInvalidSortBy_UsesDefault() throws Exception {
        DashboardStatsRow statsRow = new DashboardStatsRow(
                1L,                // playerId
                "Test Player",     // playerName
                "DEN",            // team
                "vs OPP",         // opponent
                "Points 20+",     // statLine
                new BigDecimal("80.00"),   // hitRate
                new BigDecimal("22.50")    // average
        );
        when(statsService.getDashboardStats(any(), any(), any(), any(), any()))
                .thenReturn(List.of(statsRow));

        mockMvc.perform(get("/api/dashboard/stats")
                        .param("sortBy", "invalidField"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}