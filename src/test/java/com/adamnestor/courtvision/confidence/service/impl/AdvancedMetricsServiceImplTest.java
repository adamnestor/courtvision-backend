package com.adamnestor.courtvision.confidence.service.impl;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.AdvancedGameStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvancedMetricsServiceImplTest {

    @Mock
    private AdvancedGameStatsRepository advancedStatsRepository;

    private AdvancedMetricsServiceImpl advancedMetricsService;

    private Players testPlayer;
    private Games testGame;
    private AdvancedGameStats testStats;

    @BeforeEach
    void setUp() {
        advancedMetricsService = new AdvancedMetricsServiceImpl(advancedStatsRepository);

        testPlayer = new Players();
        testPlayer.setId(1L);

        testGame = new Games();
        testGame.setId(1L);

        testStats = new AdvancedGameStats();
        testStats.setPie(new BigDecimal("0.150")); // Above league average
        testStats.setUsagePercentage(new BigDecimal("25.00")); // Above league average
        testStats.setTrueShootingPercentage(new BigDecimal("60.00")); // Above league average
        testStats.setAssistPercentage(new BigDecimal("20.00")); // Above league average
        testStats.setReboundPercentage(new BigDecimal("12.00")); // Above league average
    }

    @Test
    void calculateAdvancedImpact_WithValidStats_Points() {
        when(advancedStatsRepository.findPlayerRecentGames(testPlayer, 1))
                .thenReturn(List.of(testStats));

        BigDecimal impact = advancedMetricsService.calculateAdvancedImpact(testPlayer, testGame, StatCategory.POINTS);

        assertTrue(impact.compareTo(new BigDecimal("50.00")) > 0);
        assertEquals(2, impact.scale());
    }

    @Test
    void calculateAdvancedImpact_NoStats() {
        when(advancedStatsRepository.findPlayerRecentGames(testPlayer, 1))
                .thenReturn(Collections.emptyList());

        BigDecimal impact = advancedMetricsService.calculateAdvancedImpact(testPlayer, testGame, StatCategory.POINTS);

        assertEquals(new BigDecimal("50.00"), impact);
    }

    @Test
    void analyzePIEImpact_AboveLeagueAverage() {
        when(advancedStatsRepository.findPlayerRecentGames(testPlayer, 1))
                .thenReturn(List.of(testStats));

        BigDecimal impact = advancedMetricsService.analyzePIEImpact(testPlayer, 20, StatCategory.POINTS);

        assertTrue(impact.compareTo(new BigDecimal("50.00")) > 0);
    }

    @Test
    void analyzePIEImpact_NoStats() {
        when(advancedStatsRepository.findPlayerRecentGames(testPlayer, 1))
                .thenReturn(Collections.emptyList());

        BigDecimal impact = advancedMetricsService.analyzePIEImpact(testPlayer, 20, StatCategory.POINTS);

        assertEquals(new BigDecimal("50.00"), impact);
    }

    @Test
    void analyzeUsageRateImpact_AboveLeagueAverage() {
        when(advancedStatsRepository.findPlayerRecentGames(testPlayer, 1))
                .thenReturn(List.of(testStats));

        BigDecimal impact = advancedMetricsService.analyzeUsageRateImpact(testPlayer, testGame, StatCategory.POINTS);

        assertTrue(impact.compareTo(new BigDecimal("50.00")) > 0);
        assertTrue(impact.compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(impact.compareTo(new BigDecimal("100")) <= 0);
    }

    @Test
    void analyzeUsageRateImpact_NoStats() {
        when(advancedStatsRepository.findPlayerRecentGames(testPlayer, 1))
                .thenReturn(Collections.emptyList());

        BigDecimal impact = advancedMetricsService.analyzeUsageRateImpact(testPlayer, testGame, StatCategory.POINTS);

        assertEquals(new BigDecimal("50.00"), impact);
    }

    @Test
    void getCategoryWeights_Points() {
        // Act
        Map<String, BigDecimal> weights = advancedMetricsService.getCategoryWeights(StatCategory.POINTS);

        // Assert
        assertEquals(new BigDecimal("0.20"), weights.get("PIE"));
        assertEquals(new BigDecimal("0.30"), weights.get("USAGE"));
        assertEquals(new BigDecimal("0.50"), weights.get("EFFICIENCY"));
    }

    @Test
    void getCategoryWeights_Assists() {
        // Act
        Map<String, BigDecimal> weights = advancedMetricsService.getCategoryWeights(StatCategory.ASSISTS);

        // Assert
        assertEquals(new BigDecimal("0.10"), weights.get("PIE"));
        assertEquals(new BigDecimal("0.20"), weights.get("USAGE"));
        assertEquals(new BigDecimal("0.70"), weights.get("EFFICIENCY"));
    }

    @Test
    void getCategoryWeights_Rebounds() {
        // Act
        Map<String, BigDecimal> weights = advancedMetricsService.getCategoryWeights(StatCategory.REBOUNDS);

        // Assert
        assertEquals(new BigDecimal("0.10"), weights.get("PIE"));
        assertEquals(new BigDecimal("0.20"), weights.get("USAGE"));
        assertEquals(new BigDecimal("0.70"), weights.get("EFFICIENCY"));
    }

    @Test
    void getCategoryWeights_InvalidCategory() {
        // Assert
        assertThrows(IllegalArgumentException.class, () ->
                advancedMetricsService.getCategoryWeights(StatCategory.ALL));
    }

    @Test
    void getLatestAdvancedStats_NoStats() {
        Players player = new Players();
        when(advancedStatsRepository.findPlayerRecentGames(player, 1))
            .thenReturn(Collections.emptyList());

        assertNull(advancedMetricsService.getLatestAdvancedStats(player));
    }

    @Test
    void getLatestAdvancedStats_Success() {
        Players player = new Players();
        AdvancedGameStats stats = new AdvancedGameStats();
        when(advancedStatsRepository.findPlayerRecentGames(player, 1))
            .thenReturn(Collections.singletonList(stats));

        assertEquals(stats, advancedMetricsService.getLatestAdvancedStats(player));
    }
}