package com.adamnestor.courtvision.service.cache;

import com.adamnestor.courtvision.config.CacheConfig;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CacheWarmingServiceImpl implements CacheWarmingService {
    
    private final PlayersRepository playersRepository;
    private final GameStatsRepository gameStatsRepository;
    private final GamesRepository gamesRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheMonitoringService monitoringService;
    private final KeyGenerator keyGenerator;

    public CacheWarmingServiceImpl(
            PlayersRepository playersRepository,
            GameStatsRepository gameStatsRepository,
            GamesRepository gamesRepository,
            RedisTemplate<String, Object> redisTemplate,
            CacheMonitoringService monitoringService,
            KeyGenerator keyGenerator) {
        this.playersRepository = playersRepository;
        this.gameStatsRepository = gameStatsRepository;
        this.gamesRepository = gamesRepository;
        this.redisTemplate = redisTemplate;
        this.monitoringService = monitoringService;
        this.keyGenerator = keyGenerator;
    }

    @Override
    public void warmTodaysPlayerCache() {
        try {
            List<Players> players = playersRepository.findByStatus(PlayerStatus.ACTIVE);
            for (Players player : players) {
                List<GameStats> stats = gameStatsRepository.findPlayerRecentGames(player);
                String key = keyGenerator.playerStatsKey(player, TimePeriod.L20);
                PlayerCacheData cacheData = new PlayerCacheData(player.getId(), stats);
                redisTemplate.opsForValue().set(key, cacheData, CacheConfig.PLAYER_STATS_TTL_HOURS, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            monitoringService.recordError();
        }
    }

    @Override
    public void warmTodaysGames() {
        try {
            warmTodaysPlayerCache();
            scheduledCacheWarming();
        } catch (Exception e) {
            monitoringService.recordError();
        }
    }

    @Override
    public void warmHistoricalGames(LocalDate date) {
        try {
            List<Games> historicalGames = gamesRepository.findByGameDate(date);
            String key = keyGenerator.historicalGamesKey(date);
            redisTemplate.opsForValue().set(key, historicalGames, CacheConfig.DEFAULT_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            monitoringService.recordError();
        }
    }

    private void scheduledCacheWarming() {
        try {
            List<Games> todaysGames = gamesRepository.findByGameDateAndStatus(LocalDate.now(), GameStatus.SCHEDULED);
            String key = keyGenerator.todaysGamesKey();
            redisTemplate.opsForValue().set(key, todaysGames, CacheConfig.DEFAULT_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            monitoringService.recordError();
        }
    }
} 