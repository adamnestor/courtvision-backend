package com.adamnestor.courtvision.confidence.service.impl;

import com.adamnestor.courtvision.confidence.model.GameContext;
import com.adamnestor.courtvision.confidence.service.GameContextService;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.AdvancedGameStatsRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class GameContextServiceImpl implements GameContextService {
    private static final int SCALE = 2;
    private static final BigDecimal LEAGUE_AVG_DEF_RATING = new BigDecimal("110.00");
    private final AdvancedGameStatsRepository advancedGameStatsRepository;

    public GameContextServiceImpl(AdvancedGameStatsRepository advancedGameStatsRepository) {
        this.advancedGameStatsRepository = advancedGameStatsRepository;
    }

    @Override
    public GameContext calculateGameContext(Players player, Games game, StatCategory category) {
        boolean isHome = game.getHomeTeam().equals(player.getTeam());

        // Home court factor: 1.03 for home, 0.97 for away
        BigDecimal homeCourtFactor = isHome ?
                new BigDecimal("1.03") : new BigDecimal("0.97");

        // Get opponent's defensive rating
        Teams opponent = isHome ? game.getAwayTeam() : game.getHomeTeam();
        BigDecimal defenseRatingFactor = calculateDefenseRatingFactor(opponent, category);

        return new GameContext(homeCourtFactor, defenseRatingFactor, category);
    }

    private BigDecimal calculateDefenseRatingFactor(Teams opponent, StatCategory category) {
        // Get average defensive rating for opponent's last 10 games
        BigDecimal teamDefRating = advancedGameStatsRepository
                .findAverageTeamDefensiveRating(opponent, LocalDate.now().minusDays(30))
                .orElse(LEAGUE_AVG_DEF_RATING);

        // Normalize: Better defense (lower rating) = lower factor
        return LEAGUE_AVG_DEF_RATING.divide(teamDefRating, SCALE, RoundingMode.HALF_UP);
    }
}