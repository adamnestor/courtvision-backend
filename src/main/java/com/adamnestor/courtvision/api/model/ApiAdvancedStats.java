package com.adamnestor.courtvision.api.model;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiAdvancedStats {
    private Long id;
    private ApiPlayer player;
    private ApiTeam team;
    private ApiGame game;
    private BigDecimal pie;
    private BigDecimal pace;
    @JsonProperty("offensive_rating")
    private BigDecimal offensiveRating;
    @JsonProperty("defensive_rating")
    private BigDecimal defensiveRating;
    @JsonProperty("net_rating")
    private BigDecimal netRating;
    @JsonProperty("assist_percentage")
    private BigDecimal assistPercentage;
    @JsonProperty("assist_to_turnover")
    private BigDecimal assistToTurnover;
    @JsonProperty("assist_ratio")
    private BigDecimal assistRatio;
    @JsonProperty("offensive_rebound_percentage")
    private BigDecimal offensiveReboundPercentage;
    @JsonProperty("defensive_rebound_percentage")
    private BigDecimal defensiveReboundPercentage;
    @JsonProperty("rebound_percentage")
    private BigDecimal reboundPercentage;
    @JsonProperty("effective_field_goal_percentage")
    private BigDecimal effectiveFieldGoalPercentage;
    @JsonProperty("true_shooting_percentage")
    private BigDecimal trueShootingPercentage;
    @JsonProperty("usage_percentage")
    private BigDecimal usagePercentage;
    @JsonProperty("turnover_ratio")
    private BigDecimal turnoverRatio;

    // Constructor
    public ApiAdvancedStats() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ApiPlayer getPlayer() { return player; }
    public void setPlayer(ApiPlayer player) { this.player = player; }

    public ApiTeam getTeam() { return team; }
    public void setTeam(ApiTeam team) { this.team = team; }

    public ApiGame getGame() { return game; }
    public void setGame(ApiGame game) { this.game = game; }

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

    public BigDecimal getUsagePercentage() { return usagePercentage; }
    public void setUsagePercentage(BigDecimal usagePercentage) { this.usagePercentage = usagePercentage; }

    public BigDecimal getTurnoverRatio() { return turnoverRatio; }
    public void setTurnoverRatio(BigDecimal turnoverRatio) { this.turnoverRatio = turnoverRatio; }
} 