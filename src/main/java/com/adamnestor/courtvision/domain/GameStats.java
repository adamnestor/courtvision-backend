package com.adamnestor.courtvision.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "game_stats")
public class GameStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private Long externalId;

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
    private Integer steals;
    private Integer blocks;
    private Integer turnovers;

    @Column(name = "field_goals_made")
    private Integer fieldGoalsMade;

    @Column(name = "field_goals_attempted")
    private Integer fieldGoalsAttempted;

    @Column(name = "three_pointers_made")
    private Integer threePointersMade;

    @Column(name = "three_pointers_attempted")
    private Integer threePointersAttempted;

    @Column(name = "free_throws_made")
    private Integer freeThrowsMade;

    @Column(name = "free_throws_attempted")
    private Integer freeThrowsAttempted;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getExternalId() { return externalId; }
    public void setExternalId(Long externalId) { this.externalId = externalId; }

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

    public Integer getSteals() { return steals; }
    public void setSteals(Integer steals) { this.steals = steals; }

    public Integer getBlocks() { return blocks; }
    public void setBlocks(Integer blocks) { this.blocks = blocks; }

    public Integer getTurnovers() { return turnovers; }
    public void setTurnovers(Integer turnovers) { this.turnovers = turnovers; }

    public Integer getFieldGoalsMade() { return fieldGoalsMade; }
    public void setFieldGoalsMade(Integer fieldGoalsMade) { this.fieldGoalsMade = fieldGoalsMade; }

    public Integer getFieldGoalsAttempted() { return fieldGoalsAttempted; }
    public void setFieldGoalsAttempted(Integer fieldGoalsAttempted) { this.fieldGoalsAttempted = fieldGoalsAttempted; }

    public Integer getThreePointersMade() { return threePointersMade; }
    public void setThreePointersMade(Integer threePointersMade) { this.threePointersMade = threePointersMade; }

    public Integer getThreePointersAttempted() { return threePointersAttempted; }
    public void setThreePointersAttempted(Integer threePointersAttempted) { this.threePointersAttempted = threePointersAttempted; }

    public Integer getFreeThrowsMade() { return freeThrowsMade; }
    public void setFreeThrowsMade(Integer freeThrowsMade) { this.freeThrowsMade = freeThrowsMade; }

    public Integer getFreeThrowsAttempted() { return freeThrowsAttempted; }
    public void setFreeThrowsAttempted(Integer freeThrowsAttempted) { this.freeThrowsAttempted = freeThrowsAttempted; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
}