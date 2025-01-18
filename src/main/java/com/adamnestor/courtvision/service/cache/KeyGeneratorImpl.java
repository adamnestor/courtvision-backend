package com.adamnestor.courtvision.service.cache;

import com.adamnestor.courtvision.config.CacheConfig;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.TimePeriod;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class KeyGeneratorImpl implements KeyGenerator {
    
    @Override
    public String playerStatsKey(Players player, TimePeriod period) {
        return CacheConfig.PLAYER_KEY_PREFIX + CacheConfig.KEY_SEPARATOR + 
               CacheConfig.STATS_KEY_PREFIX + CacheConfig.KEY_SEPARATOR + 
               player.getId();
    }

    @Override
    public String historicalGamesKey(LocalDate date) {
        return CacheConfig.GAME_KEY_PREFIX + CacheConfig.KEY_SEPARATOR + date;
    }

    @Override
    public String todaysGamesKey() {
        return "today" + CacheConfig.KEY_SEPARATOR + CacheConfig.GAME_KEY_PREFIX + "s";
    }
} 