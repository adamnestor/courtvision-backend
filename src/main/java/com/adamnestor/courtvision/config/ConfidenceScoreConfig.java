package com.adamnestor.courtvision.config;

import com.adamnestor.courtvision.confidence.service.*;
import com.adamnestor.courtvision.repository.PlayersRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.adamnestor.courtvision.confidence.service.impl.ConfidenceScoreServiceImpl;
import com.adamnestor.courtvision.repository.GameStatsRepository;

@Configuration
@EntityScan(basePackages = "com.adamnestor.courtvision.domain")
@EnableJpaRepositories(basePackages = "com.adamnestor.courtvision.repository")
public class ConfidenceScoreConfig {

    /**
     * Constants for confidence score calculations
     */
    public static final int SCALE = 2;
    public static final double DECAY_FACTOR = 0.15;
    public static final int RECENT_GAMES_COUNT = 10;
    public static final String LEAGUE_AVG_DEFENSIVE_RATING = "110.0";

    /**
     * Provides the ConfidenceScoreService bean with all required dependencies
     */
    @Bean
    public ConfidenceScoreService confidenceScoreService(
            GameStatsRepository gameStatsRepository,
            PlayersRepository playersRepository,
            RestImpactService restImpactService,
            GameContextService gameContextService,
            AdvancedMetricsService advancedMetricsService,
            BlowoutRiskService blowoutRiskService) {
        return new ConfidenceScoreServiceImpl(
                gameStatsRepository,
                playersRepository,
                restImpactService,
                gameContextService,
                advancedMetricsService,
                blowoutRiskService);
    }
}