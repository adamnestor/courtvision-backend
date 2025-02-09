package com.adamnestor.courtvision.confidence.model;

import com.adamnestor.courtvision.domain.StatCategory;
import java.math.BigDecimal;

public class GameContext {
    private final BigDecimal homeCourtFactor;
    private final BigDecimal defenseRatingFactor;
    private final StatCategory category;
    private final BigDecimal overallScore;

    public GameContext(
            BigDecimal homeCourtFactor,
            BigDecimal defenseRatingFactor,
            StatCategory category) {
        this.homeCourtFactor = homeCourtFactor;
        this.defenseRatingFactor = defenseRatingFactor;
        this.category = category;
        this.overallScore = calculateOverallScore();
    }

    private BigDecimal calculateOverallScore() {
        return homeCourtFactor.multiply(new BigDecimal("0.4"))
                .add(defenseRatingFactor.multiply(new BigDecimal("0.6")));
    }

    public BigDecimal getOverallScore() {
        return overallScore;
    }

    public StatCategory getCategory() {
        return category;
    }
}