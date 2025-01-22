package com.adamnestor.courtvision.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiGameStats {
    private Long id;
    private ApiPlayer player;
    private ApiGame game;
    private String min;
    @JsonProperty("pts")
    private Integer points;
    @JsonProperty("ast")
    private Integer assists;
    @JsonProperty("reb")
    private Integer rebounds;
    @JsonProperty("stl")
    private Integer steals;
    @JsonProperty("blk")
    private Integer blocks;
    @JsonProperty("turnover")
    private Integer turnovers;
    @JsonProperty("fgm")
    private Integer fieldGoalsMade;
    @JsonProperty("fga")
    private Integer fieldGoalsAttempted;
    @JsonProperty("fg3m")
    private Integer threePointersMade;
    @JsonProperty("fg3a")
    private Integer threePointersAttempted;
    @JsonProperty("ftm")
    private Integer freeThrowsMade;
    @JsonProperty("fta")
    private Integer freeThrowsAttempted;

    // Constructor
    public ApiGameStats() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ApiPlayer getPlayer() { return player; }
    public void setPlayer(ApiPlayer player) { this.player = player; }

    public ApiGame getGame() { return game; }
    public void setGame(ApiGame game) { this.game = game; }

    public String getMin() { return min; }
    public void setMin(String min) { this.min = min; }

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
} 