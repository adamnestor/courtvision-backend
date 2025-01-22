package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.api.model.ApiGameStats;
import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.mapper.StatsMapper;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.service.BallDontLieService;
import com.adamnestor.courtvision.service.PlayerService;
import com.adamnestor.courtvision.service.StatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatsServiceImpl implements StatsService {
    
    private static final Logger logger = LoggerFactory.getLogger(StatsServiceImpl.class);
    
    private final BallDontLieService ballDontLieService;
    private final GameStatsRepository gameStatsRepository;
    private final PlayerService playerService;
    private final StatsMapper statsMapper;

    public StatsServiceImpl(
            BallDontLieService ballDontLieService,
            GameStatsRepository gameStatsRepository,
            PlayerService playerService,
            StatsMapper statsMapper) {
        this.ballDontLieService = ballDontLieService;
        this.gameStatsRepository = gameStatsRepository;
        this.playerService = playerService;
        this.statsMapper = statsMapper;
    }

    @Override
    @Transactional
    public List<GameStats> getAndUpdateGameStats(Games game) {
        logger.debug("Fetching and updating stats for game: {}", game.getId());
        List<ApiGameStats> apiStats = ballDontLieService.getGameStats(game.getExternalId());
        logger.debug("Received {} stats entries from API for game {}", apiStats.size(), game.getId());
        
        return apiStats.stream()
            .map(apiStat -> {
                Players player = null;
                if (apiStat.getPlayer() != null && apiStat.getPlayer().getId() != null) {
                    player = playerService.getAndUpdatePlayer(apiStat.getPlayer().getId());
                    if (player == null) {
                        logger.error("Could not find or create player with ID {} for game {}", 
                            apiStat.getPlayer().getId(), game.getId());
                        return null;
                    }
                } else {
                    logger.error("Received null player for game stats in game {}", game.getId());
                    logger.error("Raw stat entry: {}", apiStat);
                    return null;
                }
                
                GameStats existingStats = gameStatsRepository
                    .findByGameAndPlayer(game, player)
                    .orElse(null);
                
                if (existingStats != null) {
                    statsMapper.updateEntity(existingStats, apiStat);
                    return gameStatsRepository.save(existingStats);
                } else {
                    GameStats newStats = statsMapper.toEntity(apiStat, game, player);
                    return gameStatsRepository.save(newStats);
                }
            })
            .filter(stats -> stats != null)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<GameStats> getAndUpdatePlayerSeasonStats(Players player, Integer season) {
        logger.debug("Fetching and updating season {} stats for player: {}", season, player.getId());
        List<ApiGameStats> apiStats = ballDontLieService.getPlayerSeasonStats(player.getExternalId(), season);
        
        return apiStats.stream()
            .map(apiStat -> {
                GameStats existingStats = gameStatsRepository
                    .findByExternalId(apiStat.getId())
                    .orElse(null);
                
                if (existingStats != null) {
                    statsMapper.updateEntity(existingStats, apiStat);
                    return gameStatsRepository.save(existingStats);
                } else {
                    Games game = gameStatsRepository.findGameByExternalId(apiStat.getGame().getId())
                        .orElseThrow(() -> new IllegalStateException("Game not found: " + apiStat.getGame().getId()));
                    GameStats newStats = statsMapper.toEntity(apiStat, game, player);
                    return gameStatsRepository.save(newStats);
                }
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<GameStats> getRecentPlayerStats(Players player, int limit) {
        return gameStatsRepository.findByPlayerOrderByGameDateDesc(player, limit);
    }

    @Override
    public List<GameStats> getGameStats(Games game) {
        return gameStatsRepository.findByGame(game);
    }
} 