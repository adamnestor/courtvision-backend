package com.adamnestor.courtvision.domain;

import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private LocalDateTime lastCalculated;

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

    public LocalDateTime getLastCalculated() { return lastCalculated; }
    public void setLastCalculated(LocalDateTime lastCalculated) { this.lastCalculated = lastCalculated; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}