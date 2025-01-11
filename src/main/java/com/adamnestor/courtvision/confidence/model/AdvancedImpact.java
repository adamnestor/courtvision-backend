package com.adamnestor.courtvision.confidence.model;

import com.adamnestor.courtvision.domain.StatCategory;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Represents the impact of advanced metrics on performance prediction
 */
public class AdvancedImpact {
    private final BigDecimal pieImpact;
    private final BigDecimal usageRateImpact;
    private final BigDecimal efficiencyImpact;  // TS% for points, AST% for assists, REB% for rebounds
    private final BigDecimal overallScore;
    private final StatCategory category;
    private final Map<String, BigDecimal> componentWeights;

    public AdvancedImpact(
            BigDecimal pieImpact,
            BigDecimal usageRateImpact,
            BigDecimal efficiencyImpact,
            StatCategory category,
            Map<String, BigDecimal> weights) {
        this.pieImpact = pieImpact;
        this.usageRateImpact = usageRateImpact;
        this.efficiencyImpact = efficiencyImpact;
        this.category = category;
        this.componentWeights = weights;
        this.overallScore = calculateOverallScore();
    }

    private BigDecimal calculateOverallScore() {
        return pieImpact.multiply(componentWeights.get("PIE"))
                .add(usageRateImpact.multiply(componentWeights.get("USAGE")))
                .add(efficiencyImpact.multiply(componentWeights.get("EFFICIENCY")));
    }

    // Getters
    public BigDecimal getPieImpact() { return pieImpact; }
    public BigDecimal getUsageRateImpact() { return usageRateImpact; }
    public BigDecimal getEfficiencyImpact() { return efficiencyImpact; }
    public BigDecimal getOverallScore() { return overallScore; }
    public StatCategory getCategory() { return category; }
    public Map<String, BigDecimal> getComponentWeights() { return componentWeights; }
}