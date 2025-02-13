package com.adamnestor.courtvision.api.model;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiGame {
    private Long id;
    private LocalDate date;
    private Integer season;
    private String status;
    private Integer period;
    private String time;
    private Boolean postseason;
    @JsonProperty("home_team_score")
    private Integer homeTeamScore;
    @JsonProperty("visitor_team_score")
    private Integer visitorTeamScore;
    @JsonProperty("home_team")
    private ApiTeam homeTeam;
    @JsonProperty("visitor_team")
    private ApiTeam visitorTeam;

    // Constructor
    public ApiGame() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Integer getSeason() { return season; }
    public void setSeason(Integer season) { this.season = season; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getPeriod() { return period; }
    public void setPeriod(Integer period) { this.period = period; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public Integer getHomeTeamScore() { return homeTeamScore; }
    public void setHomeTeamScore(Integer homeTeamScore) { this.homeTeamScore = homeTeamScore; }

    public Integer getVisitorTeamScore() { return visitorTeamScore; }
    public void setVisitorTeamScore(Integer visitorTeamScore) { this.visitorTeamScore = visitorTeamScore; }

    public ApiTeam getHomeTeam() { return homeTeam; }
    public void setHomeTeam(ApiTeam homeTeam) { this.homeTeam = homeTeam; }

    public ApiTeam getVisitorTeam() { return visitorTeam; }
    public void setVisitorTeam(ApiTeam visitorTeam) { this.visitorTeam = visitorTeam; }

    public Boolean getPostseason() { return postseason; }
    public void setPostseason(Boolean postseason) { this.postseason = postseason; }
} 