package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.api.model.ApiAdvancedStats;
import com.adamnestor.courtvision.domain.AdvancedGameStats;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.mapper.AdvancedStatsMapper;
import com.adamnestor.courtvision.repository.AdvancedGameStatsRepository;
import com.adamnestor.courtvision.service.AdvancedStatsService;
import com.adamnestor.courtvision.service.BallDontLieService;
import com.adamnestor.courtvision.service.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdvancedStatsServiceImpl implements AdvancedStatsService {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedStatsServiceImpl.class);
    
    private final BallDontLieService ballDontLieService;
    private final AdvancedGameStatsRepository advancedStatsRepository;
    private final AdvancedStatsMapper advancedStatsMapper;
    private final PlayerService playerService;

    public AdvancedStatsServiceImpl(
            BallDontLieService ballDontLieService,
            AdvancedGameStatsRepository advancedStatsRepository,
            AdvancedStatsMapper advancedStatsMapper,
            PlayerService playerService) {
        this.ballDontLieService = ballDontLieService;
        this.advancedStatsRepository = advancedStatsRepository;
        this.advancedStatsMapper = advancedStatsMapper;
        this.playerService = playerService;
    }

    @Override
    @Transactional
    public List<AdvancedGameStats> getAndUpdateGameAdvancedStats(Games game) {
        logger.debug("Fetching and updating advanced stats for game: {}", game.getId());
        List<ApiAdvancedStats> apiStats = ballDontLieService.getAdvancedGameStats(game.getExternalId());
        
        return apiStats.stream()
            .map(apiStat -> {
                Players player = null;
                if (apiStat.getPlayer() != null && apiStat.getPlayer().getId() != null) {
                    player = playerService.getAndUpdatePlayer(apiStat.getPlayer().getId());
                    if (player == null) {
                        logger.warn("Could not find or create player with ID {} for game {}", 
                            apiStat.getPlayer().getId(), game.getId());
                        return null;
                    }
                } else {
                    logger.warn("Received null player ID for advanced game stats in game {}", game.getId());
                    return null;
                }

                AdvancedGameStats existingStats = advancedStatsRepository
                    .findByPlayerAndGame(player, game)
                    .orElse(null);
                
                if (existingStats != null) {
                    advancedStatsMapper.updateEntity(existingStats, apiStat);
                    return advancedStatsRepository.save(existingStats);
                } else {
                    AdvancedGameStats newStats = advancedStatsMapper.toEntity(apiStat, game, player);
                    return advancedStatsRepository.save(newStats);
                }
            })
            .filter(stats -> stats != null)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<AdvancedGameStats> getAndUpdatePlayerSeasonAdvancedStats(Players player, Integer season) {
        logger.debug("Fetching and updating season {} advanced stats for player: {}", 
                    season, player.getId());
        List<ApiAdvancedStats> apiStats = ballDontLieService.getAdvancedSeasonStats(
            player.getExternalId(), 
            season
        );
        
        return apiStats.stream()
            .map(apiStat -> {
                Games targetGame = new Games();
                targetGame.setExternalId(apiStat.getGame().getId());
                Optional<AdvancedGameStats> existingStats = advancedStatsRepository
                    .findByPlayerAndGame(player, targetGame);
                
                if (existingStats.isPresent()) {
                    advancedStatsMapper.updateEntity(existingStats.get(), apiStat);
                    return advancedStatsRepository.save(existingStats.get());
                } else {
                    AdvancedGameStats newStats = advancedStatsMapper.toEntity(
                        apiStat,
                        targetGame,
                        player
                    );
                    return advancedStatsRepository.save(newStats);
                }
            })
            .collect(Collectors.toList());
    }

    @Override
    public Optional<AdvancedGameStats> getAdvancedStats(Games game, Players player) {
        return advancedStatsRepository.findByPlayerAndGame(player, game);
    }

    @Override
    public List<AdvancedGameStats> getRecentAdvancedStats(Players player, int limit) {
        return advancedStatsRepository.findPlayerRecentGames(player, limit);
    }
} 