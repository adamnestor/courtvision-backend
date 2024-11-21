package com.adamnestor.courtvision.domain;

import com.adamnestor.courtvision.domain.StatCategory;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Column(name = "hit_rate_at_pick")
    private BigDecimal hitRateAtPick;

    private Boolean result;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}