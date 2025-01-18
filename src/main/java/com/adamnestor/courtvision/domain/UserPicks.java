package com.adamnestor.courtvision.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "user_picks")
public class UserPicks {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Players player;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    private Games game;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatCategory category;

    @Column(nullable = false)
    private Integer threshold;

    @Column(name = "hit_rate_at_pick", precision = 5, scale = 2)
    private BigDecimal hitRateAtPick;

    private Boolean result;

    @Column(name = "result_value")
    private Integer resultValue;

    @Column(name = "parlay_result")
    private Boolean parlayResult;

    @Column(name = "parlay_id")
    private String parlayId;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @Column(name = "created_time")
    private String createdTime;

    // Constructor
    public UserPicks() {
        this.createdAt = LocalDate.now();
        this.createdTime = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"));
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Users getUser() { return user; }
    public void setUser(Users user) { this.user = user; }

    public Players getPlayer() { return player; }
    public void setPlayer(Players player) { this.player = player; }

    public Games getGame() { return game; }
    public void setGame(Games game) { this.game = game; }

    public StatCategory getCategory() { return category; }
    public void setCategory(StatCategory category) { this.category = category; }

    public Integer getThreshold() { return threshold; }
    public void setThreshold(Integer threshold) { this.threshold = threshold; }

    public BigDecimal getHitRateAtPick() { return hitRateAtPick; }
    public void setHitRateAtPick(BigDecimal hitRateAtPick) { this.hitRateAtPick = hitRateAtPick; }

    public Boolean getResult() { return result; }
    public void setResult(Boolean result) { this.result = result; }

    public Integer getResultValue() { return resultValue; }
    public void setResultValue(Integer resultValue) { this.resultValue = resultValue; }

    public Boolean getParlayResult() { return parlayResult; }
    public void setParlayResult(Boolean parlayResult) { this.parlayResult = parlayResult; }

    public String getParlayId() { return parlayId; }
    public void setParlayId(String parlayId) { this.parlayId = parlayId; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public String getCreatedTime() { return createdTime; }
    public void setCreatedTime(String createdTime) { this.createdTime = createdTime; }
}