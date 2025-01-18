package com.adamnestor.courtvision.domain;

import java.io.Serializable;
import java.util.List;

public class PlayerCacheData implements Serializable {
    private Long playerId;
    private List<GameStats> stats;

    public PlayerCacheData(Long playerId, List<GameStats> stats) {
        this.playerId = playerId;
        this.stats = stats;
    }

    public Long getPlayerId() { return playerId; }
    public List<GameStats> getStats() { return stats; }
} 