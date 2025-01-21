package com.adamnestor.courtvision.api.model;

import java.math.BigDecimal;

public class ApiAdvancedStats {
    private Long id;
    private Long playerId;
    private Long gameId;
    private BigDecimal pie;
    private BigDecimal pace;
    private BigDecimal offensiveRating;
    private BigDecimal defensiveRating;
    private BigDecimal netRating;
    private BigDecimal assistPercentage;
    private BigDecimal assistToTurnover;
    private BigDecimal assistRatio;
    private BigDecimal offensiveReboundPercentage;
    private BigDecimal defensiveReboundPercentage;
    private BigDecimal reboundPercentage;
    private BigDecimal effectiveFieldGoalPercentage;
    private BigDecimal trueShootingPercentage;
    private BigDecimal usageRate;

    // Constructor
    public ApiAdvancedStats() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }

    public BigDecimal getPie() { return pie; }
    public void setPie(BigDecimal pie) { this.pie = pie; }

    public BigDecimal getPace() { return pace; }
    public void setPace(BigDecimal pace) { this.pace = pace; }

    public BigDecimal getOffensiveRating() { return offensiveRating; }
    public void setOffensiveRating(BigDecimal offensiveRating) { this.offensiveRating = offensiveRating; }

    public BigDecimal getDefensiveRating() { return defensiveRating; }
    public void setDefensiveRating(BigDecimal defensiveRating) { this.defensiveRating = defensiveRating; }

    public BigDecimal getNetRating() { return netRating; }
    public void setNetRating(BigDecimal netRating) { this.netRating = netRating; }

    public BigDecimal getAssistPercentage() { return assistPercentage; }
    public void setAssistPercentage(BigDecimal assistPercentage) { this.assistPercentage = assistPercentage; }

    public BigDecimal getAssistToTurnover() { return assistToTurnover; }
    public void setAssistToTurnover(BigDecimal assistToTurnover) { this.assistToTurnover = assistToTurnover; }

    public BigDecimal getAssistRatio() { return assistRatio; }
    public void setAssistRatio(BigDecimal assistRatio) { this.assistRatio = assistRatio; }

    public BigDecimal getOffensiveReboundPercentage() { return offensiveReboundPercentage; }
    public void setOffensiveReboundPercentage(BigDecimal offensiveReboundPercentage) { 
        this.offensiveReboundPercentage = offensiveReboundPercentage; 
    }

    public BigDecimal getDefensiveReboundPercentage() { return defensiveReboundPercentage; }
    public void setDefensiveReboundPercentage(BigDecimal defensiveReboundPercentage) { 
        this.defensiveReboundPercentage = defensiveReboundPercentage; 
    }

    public BigDecimal getReboundPercentage() { return reboundPercentage; }
    public void setReboundPercentage(BigDecimal reboundPercentage) { this.reboundPercentage = reboundPercentage; }

    public BigDecimal getEffectiveFieldGoalPercentage() { return effectiveFieldGoalPercentage; }
    public void setEffectiveFieldGoalPercentage(BigDecimal effectiveFieldGoalPercentage) { 
        this.effectiveFieldGoalPercentage = effectiveFieldGoalPercentage; 
    }

    public BigDecimal getTrueShootingPercentage() { return trueShootingPercentage; }
    public void setTrueShootingPercentage(BigDecimal trueShootingPercentage) { 
        this.trueShootingPercentage = trueShootingPercentage; 
    }

    public BigDecimal getUsageRate() { return usageRate; }
    public void setUsageRate(BigDecimal usageRate) { this.usageRate = usageRate; }
} 