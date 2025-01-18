package com.adamnestor.courtvision.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "game_stats")
public class GameStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Players player;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    private Games game;

    @Column(name = "minutes_played")
    private String minutesPlayed;

    private Integer points;
    private Integer assists;
    private Integer rebounds;

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

    public Games getGame() { return game; }
    public void setGame(Games game) { this.game = game; }

    public String getMinutesPlayed() { return minutesPlayed; }
    public void setMinutesPlayed(String minutesPlayed) { this.minutesPlayed = minutesPlayed; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public Integer getAssists() { return assists; }
    public void setAssists(Integer assists) { this.assists = assists; }

    public Integer getRebounds() { return rebounds; }
    public void setRebounds(Integer rebounds) { this.rebounds = rebounds; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
}