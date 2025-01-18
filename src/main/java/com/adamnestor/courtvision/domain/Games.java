package com.adamnestor.courtvision.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "games")
public class Games {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private Long externalId;

    @ManyToOne
    @JoinColumn(name = "home_team_id", nullable = false)
    private Teams homeTeam;

    @ManyToOne
    @JoinColumn(name = "away_team_id", nullable = false)
    private Teams awayTeam;

    @Column(name = "game_date")
    private LocalDate gameDate;

    @Column(name = "game_time")
    private String gameTime;

    @Column(nullable = false)
    private Integer season;

    @Column(name = "status")
    private String status;

    private Integer period;

    @Column(name = "home_team_score")
    private Integer homeTeamScore;

    @Column(name = "away_team_score")
    private Integer awayTeamScore;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
        updatedAt = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDate.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getExternalId() { return externalId; }
    public void setExternalId(Long externalId) { this.externalId = externalId; }

    public Teams getHomeTeam() { return homeTeam; }
    public void setHomeTeam(Teams homeTeam) { this.homeTeam = homeTeam; }

    public Teams getAwayTeam() { return awayTeam; }
    public void setAwayTeam(Teams awayTeam) { this.awayTeam = awayTeam; }

    public LocalDate getGameDate() { return gameDate; }
    public void setGameDate(LocalDateTime dateTime) {
        ZoneId easternZone = ZoneId.of("America/New_York");
        ZonedDateTime easternTime = dateTime.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(easternZone);
        this.gameDate = easternTime.toLocalDate();
        this.gameTime = easternTime.toLocalTime().format(DateTimeFormatter.ofPattern("h:mm a z"));
    }

    public Integer getSeason() { return season; }
    public void setSeason(Integer season) { this.season = season; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getPeriod() { return period; }
    public void setPeriod(Integer period) { this.period = period; }

    public Integer getHomeTeamScore() { return homeTeamScore; }
    public void setHomeTeamScore(Integer homeTeamScore) { this.homeTeamScore = homeTeamScore; }

    public Integer getAwayTeamScore() { return awayTeamScore; }
    public void setAwayTeamScore(Integer awayTeamScore) { this.awayTeamScore = awayTeamScore; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public LocalDate getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDate updatedAt) { this.updatedAt = updatedAt; }

    public String getGameTime() { return gameTime; }
    public void setGameTime(String gameTime) { this.gameTime = gameTime; }
}