package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.api.model.ApiGameStats;
import com.adamnestor.courtvision.api.model.ApiAdvancedStats;
import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.AdvancedGameStats;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.mapper.StatsMapper;
import com.adamnestor.courtvision.mapper.AdvancedStatsMapper;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.AdvancedGameStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
public class StatsService {

    private static final Logger logger = LoggerFactory.getLogger(StatsService.class);

    private final BallDontLieService ballDontLieService;
    private final GameStatsRepository gameStatsRepository;
    private final AdvancedGameStatsRepository advancedGameStatsRepository;
    private final PlayerService playerService;
    private final StatsMapper statsMapper;
    private final AdvancedStatsMapper advancedStatsMapper;

    public StatsService(
            BallDontLieService ballDontLieService,
            GameStatsRepository gameStatsRepository,
            AdvancedGameStatsRepository advancedGameStatsRepository,
            PlayerService playerService,
            StatsMapper statsMapper,
            AdvancedStatsMapper advancedStatsMapper) {
        this.ballDontLieService = ballDontLieService;
        this.gameStatsRepository = gameStatsRepository;
        this.advancedGameStatsRepository = advancedGameStatsRepository;
        this.playerService = playerService;
        this.statsMapper = statsMapper;
        this.advancedStatsMapper = advancedStatsMapper;
    }

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

    public List<GameStats> getRecentPlayerStats(Players player, int limit) {
        return gameStatsRepository.findByPlayerOrderByGameDateDesc(player, limit);
    }

    public List<GameStats> getGameStats(Games game) {
        return gameStatsRepository.findByGame(game);
    }

    @Transactional
    public List<AdvancedGameStats> getAndUpdateGameAdvancedStats(Games game) {
        try {
            List<ApiAdvancedStats> apiStats = ballDontLieService.getAdvancedGameStats(game.getExternalId());
            List<AdvancedGameStats> stats = new ArrayList<>();

            for (ApiAdvancedStats apiStat : apiStats) {
                Players player = playerService.getAndUpdatePlayer(apiStat.getPlayer().getId());
                if (player == null) {
                    logger.warn("Skipping advanced stat - no player found for game {}", game.getId());
                    continue;
                }
                AdvancedGameStats stat = advancedStatsMapper.toEntity(apiStat, game, player);
                stat.setGame(game);
                stats.add(advancedGameStatsRepository.save(stat));
            }

            return stats;
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity error for game {}: {}", game.getId(), e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            logger.error("Error processing advanced stats for game {}: {}", game.getId(), e.getMessage());
            throw e;
        }
    }

    public List<GameStats> getFilteredPlayerStats(
            Players player,
            int numGames,
            StatCategory category,
            Integer threshold
    ) {
        return gameStatsRepository.findPlayerRecentGames(player).stream()
                .limit(numGames)
                .filter(stat -> {
                    int value = switch (category) {
                        case POINTS -> stat.getPoints();
                        case ASSISTS -> stat.getAssists();
                        case REBOUNDS -> stat.getRebounds();
                        default -> 0;
                    };
                    return value >= (threshold != null ? threshold : category.getDefaultThreshold());
                })
                .collect(Collectors.toList());
    }
}