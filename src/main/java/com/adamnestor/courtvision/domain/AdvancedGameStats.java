package com.adamnestor.courtvision.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "advanced_game_stats")
public class AdvancedGameStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Players player;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    private Games game;

    // Core Advanced Stats from BallDontLie API
    @Column(precision = 4, scale = 3)
    private BigDecimal pie;  // Player Impact Estimate: -0.100 to 0.300

    @Column(precision = 5, scale = 2)
    private BigDecimal pace;  // Possessions per 48 minutes: 90.00 to 120.00

    @Column(name = "assist_percentage", precision = 5, scale = 2)
    private BigDecimal assistPercentage;  // 0.00 to 100.00

    @Column(name = "assist_ratio", precision = 5, scale = 2)
    private BigDecimal assistRatio;  // 0.00 to 100.00

    @Column(name = "assist_to_turnover", precision = 4, scale = 2)
    private BigDecimal assistToTurnover;  // 0.00 to 10.00

    @Column(name = "defensive_rating", precision = 5, scale = 2)
    private BigDecimal defensiveRating;  // 50.00 to 150.00

    @Column(name = "defensive_rebound_percentage", precision = 5, scale = 2)
    private BigDecimal defensiveReboundPercentage;  // 0.00 to 100.00

    @Column(name = "effective_fg_percentage", precision = 5, scale = 2)
    private BigDecimal effectiveFieldGoalPercentage;  // 0.00 to 100.00

    @Column(name = "net_rating", precision = 5, scale = 2)
    private BigDecimal netRating;  // -50.00 to 50.00

    @Column(name = "offensive_rating", precision = 5, scale = 2)
    private BigDecimal offensiveRating;  // 50.00 to 150.00

    @Column(name = "offensive_rebound_percentage", precision = 5, scale = 2)
    private BigDecimal offensiveReboundPercentage;  // 0.00 to 100.00

    @Column(name = "rebound_percentage", precision = 5, scale = 2)
    private BigDecimal reboundPercentage;  // 0.00 to 100.00

    @Column(name = "true_shooting_percentage", precision = 5, scale = 2)
    private BigDecimal trueShootingPercentage;  // 0.00 to 100.00

    @Column(name = "turnover_ratio", precision = 5, scale = 2)
    private BigDecimal turnoverRatio;  // 0.00 to 100.00

    @Column(name = "usage_percentage", precision = 5, scale = 2)
    private BigDecimal usagePercentage;  // 0.00 to 100.00

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Players getPlayer() { return player; }
    public void setPlayer(Players player) { this.player = player; }

    public Games getGame() { return game; }
    public void setGame(Games game) { this.game = game; }

    public BigDecimal getPie() { return pie; }
    public void setPie(BigDecimal pie) { this.pie = pie; }

    public BigDecimal getPace() { return pace; }
    public void setPace(BigDecimal pace) { this.pace = pace; }

    public BigDecimal getAssistPercentage() { return assistPercentage; }
    public void setAssistPercentage(BigDecimal assistPercentage) {
        this.assistPercentage = assistPercentage;
    }

    public BigDecimal getAssistRatio() { return assistRatio; }
    public void setAssistRatio(BigDecimal assistRatio) {
        this.assistRatio = assistRatio;
    }

    public BigDecimal getAssistToTurnover() { return assistToTurnover; }
    public void setAssistToTurnover(BigDecimal assistToTurnover) {
        this.assistToTurnover = assistToTurnover;
    }

    public BigDecimal getDefensiveRating() { return defensiveRating; }
    public void setDefensiveRating(BigDecimal defensiveRating) {
        this.defensiveRating = defensiveRating;
    }

    public BigDecimal getDefensiveReboundPercentage() { return defensiveReboundPercentage; }
    public void setDefensiveReboundPercentage(BigDecimal defensiveReboundPercentage) {
        this.defensiveReboundPercentage = defensiveReboundPercentage;
    }

    public BigDecimal getEffectiveFieldGoalPercentage() { return effectiveFieldGoalPercentage; }
    public void setEffectiveFieldGoalPercentage(BigDecimal effectiveFieldGoalPercentage) {
        this.effectiveFieldGoalPercentage = effectiveFieldGoalPercentage;
    }

    public BigDecimal getNetRating() { return netRating; }
    public void setNetRating(BigDecimal netRating) {
        this.netRating = netRating;
    }

    public BigDecimal getOffensiveRating() { return offensiveRating; }
    public void setOffensiveRating(BigDecimal offensiveRating) {
        this.offensiveRating = offensiveRating;
    }

    public BigDecimal getOffensiveReboundPercentage() { return offensiveReboundPercentage; }
    public void setOffensiveReboundPercentage(BigDecimal offensiveReboundPercentage) {
        this.offensiveReboundPercentage = offensiveReboundPercentage;
    }

    public BigDecimal getReboundPercentage() { return reboundPercentage; }
    public void setReboundPercentage(BigDecimal reboundPercentage) {
        this.reboundPercentage = reboundPercentage;
    }

    public BigDecimal getTrueShootingPercentage() { return trueShootingPercentage; }
    public void setTrueShootingPercentage(BigDecimal trueShootingPercentage) {
        this.trueShootingPercentage = trueShootingPercentage;
    }

    public BigDecimal getTurnoverRatio() { return turnoverRatio; }
    public void setTurnoverRatio(BigDecimal turnoverRatio) {
        this.turnoverRatio = turnoverRatio;
    }

    public BigDecimal getUsagePercentage() { return usagePercentage; }
    public void setUsagePercentage(BigDecimal usagePercentage) {
        this.usagePercentage = usagePercentage;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}