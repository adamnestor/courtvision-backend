package com.adamnestor.courtvision.api.model;

public class ApiGameStats {
    private Long id;
    private Long playerId;
    private Long gameId;
    private String min;
    private Integer points;
    private Integer assists;
    private Integer rebounds;
    private Integer steals;
    private Integer blocks;
    private Integer turnovers;
    private Integer fgm;
    private Integer fga;
    private Integer fg3m;
    private Integer fg3a;
    private Integer ftm;
    private Integer fta;

    // Constructor
    public ApiGameStats() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }

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

    public Integer getFgm() { return fgm; }
    public void setFgm(Integer fgm) { this.fgm = fgm; }

    public Integer getFga() { return fga; }
    public void setFga(Integer fga) { this.fga = fga; }

    public Integer getFg3m() { return fg3m; }
    public void setFg3m(Integer fg3m) { this.fg3m = fg3m; }

    public Integer getFg3a() { return fg3a; }
    public void setFg3a(Integer fg3a) { this.fg3a = fg3a; }

    public Integer getFtm() { return ftm; }
    public void setFtm(Integer ftm) { this.ftm = ftm; }

    public Integer getFta() { return fta; }
    public void setFta(Integer fta) { this.fta = fta; }
} 