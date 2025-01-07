package com.adamnestor.courtvision.confidence.model;

import com.adamnestor.courtvision.domain.StatCategory;
import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;

public class GameContext {
    private static final Map<StatCategory, Map<String, BigDecimal>> CATEGORY_WEIGHTS;

    static {
        CATEGORY_WEIGHTS = new HashMap<>();

        Map<String, BigDecimal> standardWeights = Map.of(
                "MATCHUP", new BigDecimal("0.40"),
                "DEFENSE", new BigDecimal("0.35"),
                "PACE", new BigDecimal("0.15"),
                "VENUE", new BigDecimal("0.10")
        );

        // Using same weights for all categories initially
        CATEGORY_WEIGHTS.put(StatCategory.POINTS, standardWeights);
        CATEGORY_WEIGHTS.put(StatCategory.ASSISTS, standardWeights);
        CATEGORY_WEIGHTS.put(StatCategory.REBOUNDS, standardWeights);
    }

    private final BigDecimal matchupImpact;
    private final BigDecimal defensiveImpact;
    private final BigDecimal paceImpact;
    private final BigDecimal venueImpact;
    private final StatCategory category;
    private final BigDecimal overallScore;

    public GameContext(
            BigDecimal matchupImpact,
            BigDecimal defensiveImpact,
            BigDecimal paceImpact,
            BigDecimal venueImpact,
            StatCategory category) {
        this.matchupImpact = matchupImpact;
        this.defensiveImpact = defensiveImpact;
        this.paceImpact = paceImpact;
        this.venueImpact = venueImpact;
        this.category = category;
        this.overallScore = calculateOverallScore();
    }

    private BigDecimal calculateOverallScore() {
        Map<String, BigDecimal> weights = CATEGORY_WEIGHTS.get(category);

        return matchupImpact.multiply(weights.get("MATCHUP"))
                .add(defensiveImpact.multiply(weights.get("DEFENSE")))
                .add(paceImpact.multiply(weights.get("PACE")))
                .add(venueImpact.multiply(weights.get("VENUE")));
    }

    // Getters
    public BigDecimal getMatchupImpact() { return matchupImpact; }
    public BigDecimal getDefensiveImpact() { return defensiveImpact; }
    public BigDecimal getPaceImpact() { return paceImpact; }
    public BigDecimal getVenueImpact() { return venueImpact; }
    public StatCategory getCategory() { return category; }
    public BigDecimal getOverallScore() { return overallScore; }
}