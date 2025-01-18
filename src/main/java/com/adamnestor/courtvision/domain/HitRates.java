package com.adamnestor.courtvision.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "hit_rates")
public class HitRates {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Players player;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatCategory category;

    @Column(nullable = false)
    private Integer threshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_period", nullable = false)
    private TimePeriod timePeriod;

    @Column(name = "hit_rate", nullable = false)
    private BigDecimal hitRate;

    @Column(nullable = false)
    private BigDecimal average;

    @Column(name = "games_counted", nullable = false)
    private Integer gamesCounted;

    @Column(name = "last_calculated", nullable = false)
    private LocalDate lastCalculated;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Players getPlayer() { return player; }
    public void setPlayer(Players player) { this.player = player; }

    public StatCategory getCategory() { return category; }
    public void setCategory(StatCategory category) { this.category = category; }

    public Integer getThreshold() { return threshold; }
    public void setThreshold(Integer threshold) { this.threshold = threshold; }

    public TimePeriod getTimePeriod() { return timePeriod; }
    public void setTimePeriod(TimePeriod timePeriod) { this.timePeriod = timePeriod; }

    public BigDecimal getHitRate() { return hitRate; }
    public void setHitRate(BigDecimal hitRate) { this.hitRate = hitRate; }

    public BigDecimal getAverage() { return average; }
    public void setAverage(BigDecimal average) { this.average = average; }

    public Integer getGamesCounted() { return gamesCounted; }
    public void setGamesCounted(Integer gamesCounted) { this.gamesCounted = gamesCounted; }

    public LocalDate getLastCalculated() { return lastCalculated; }
    public void setLastCalculated(LocalDate lastCalculated) { this.lastCalculated = lastCalculated; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
}