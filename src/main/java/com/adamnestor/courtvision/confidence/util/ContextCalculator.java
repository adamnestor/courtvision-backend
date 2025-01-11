package com.adamnestor.courtvision.confidence.util;

import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.Teams;
import com.adamnestor.courtvision.domain.StatCategory;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ContextCalculator {
    private static final int SCALE = 2;
    private static final BigDecimal LEAGUE_AVG_PACE = new BigDecimal("100.00");
    private static final BigDecimal HOME_BASELINE = new BigDecimal("1.02");
    private static final BigDecimal AWAY_BASELINE = new BigDecimal("0.98");

    private ContextCalculator() {
        throw new IllegalStateException("Utility class");
    }

    public static BigDecimal calculatePaceFactor(BigDecimal teamPace, BigDecimal opponentPace) {
        BigDecimal avgPace = teamPace.add(opponentPace).divide(new BigDecimal("2"), SCALE, RoundingMode.HALF_UP);
        return avgPace.divide(LEAGUE_AVG_PACE, SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateVenueFactor(Games game, Players player) {
        boolean isHome = game.getHomeTeam().equals(player.getTeam());
        return isHome ? HOME_BASELINE : AWAY_BASELINE;
    }

    public static BigDecimal calculateDefensiveImpact(
            BigDecimal opponentDefRating,
            StatCategory category) {
        BigDecimal leagueAvg = new BigDecimal("110.0"); // League average defensive rating
        return leagueAvg.divide(opponentDefRating, SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal normalizeScore(BigDecimal rawScore) {
        return rawScore.multiply(new BigDecimal("100"))
                .min(new BigDecimal("100"))
                .max(BigDecimal.ZERO)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }
}