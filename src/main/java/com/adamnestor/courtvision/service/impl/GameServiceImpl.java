package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.api.model.ApiGame;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.mapper.GameMapper;
import com.adamnestor.courtvision.repository.GamesRepository;
import com.adamnestor.courtvision.service.BallDontLieService;
import com.adamnestor.courtvision.service.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GameServiceImpl implements GameService {
    
    private static final Logger logger = LoggerFactory.getLogger(GameServiceImpl.class);
    
    private final BallDontLieService ballDontLieService;
    private final GamesRepository gamesRepository;
    private final GameMapper gameMapper;

    public GameServiceImpl(
            BallDontLieService ballDontLieService,
            GamesRepository gamesRepository,
            GameMapper gameMapper) {
        this.ballDontLieService = ballDontLieService;
        this.gamesRepository = gamesRepository;
        this.gameMapper = gameMapper;
    }

    @Override
    @Transactional
    public List<Games> getAndUpdateGames(LocalDate date) {
        logger.debug("Fetching and updating games for date: {}", date);
        List<ApiGame> apiGames = ballDontLieService.getGames(date);
        
        return apiGames.stream()
            .map(apiGame -> {
                logger.debug("Processing API game with external ID: {}", apiGame.getId());
                Games existingGame = gamesRepository
                    .findByExternalId(apiGame.getId())
                    .orElse(null);
                
                if (existingGame != null) {
                    logger.debug("Found existing game - ID: {}, externalId: {}", 
                        existingGame.getId(), existingGame.getExternalId());
                    gameMapper.updateEntity(existingGame, apiGame);
                    return gamesRepository.save(existingGame);
                } else {
                    logger.debug("Creating new game for external ID: {}", apiGame.getId());
                    Games newGame = gameMapper.toEntity(apiGame);
                    return gamesRepository.save(newGame);
                }
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<Games> getAndUpdateGamesBySeason(Integer season) {
        logger.debug("Fetching and updating games for season: {}", season);
        // For a season, fetch games from October to June
        List<ApiGame> apiGames = new ArrayList<>();
        for (int month = 10; month <= 12; month++) {
            apiGames.addAll(ballDontLieService.getGamesByYearMonth(season, month));
        }
        for (int month = 1; month <= 6; month++) {
            apiGames.addAll(ballDontLieService.getGamesByYearMonth(season + 1, month));
        }
        
        return apiGames.stream()
            .map(apiGame -> {
                Games existingGame = gamesRepository.findByExternalId(apiGame.getId())
                    .orElse(null);
                
                if (existingGame != null) {
                    // Update fields but preserve the ID
                    Games updatedGame = gameMapper.toEntity(apiGame);
                    updatedGame.setId(existingGame.getId());
                    return gamesRepository.save(updatedGame);
                } else {
                    Games newGame = gameMapper.toEntity(apiGame);
                    return gamesRepository.save(newGame);
                }
            })
            .collect(Collectors.toList());
    }

    @Override
    public Games findByExternalId(Long externalId) {
        return gamesRepository.findByExternalId(externalId)
            .orElse(null);
    }

    @Override
    public List<Games> getTodaysGames() {
        logger.debug("Fetching today's games");
        LocalDate today = LocalDate.now();
        
        // First check if we need to fetch/update today's games
        List<Games> todaysGames = gamesRepository.findByGameDate(today);
        
        if (todaysGames.isEmpty()) {
            // If no games found, try to fetch them from the API
            logger.debug("No games found in database for today, fetching from API");
            todaysGames = getAndUpdateGames(today);
        } else {
            // If games exist, check if we need to update their status
            boolean needsUpdate = todaysGames.stream()
                .anyMatch(game -> !"Final".equals(game.getStatus()));
                
            if (needsUpdate) {
                logger.debug("Found non-final games, updating from API");
                todaysGames = getAndUpdateGames(today);
            }
        }
        
        logger.info("Found {} games for today ({})", todaysGames.size(), today);
        return todaysGames;
    }

    @Override
    @Transactional
    public List<Games> getGamesByDateRange(LocalDate startDate, LocalDate endDate) {
        logger.info("Fetching games between {} and {}", startDate, endDate);
        List<ApiGame> apiGames = ballDontLieService.getGamesByDateRange(startDate, endDate);
        
        return apiGames.stream()
            .map(apiGame -> {
                Games existingGame = gamesRepository.findByExternalId(apiGame.getId())
                    .orElse(null);
                
                // Validate required team data
                if (apiGame.getHomeTeam() == null || apiGame.getVisitorTeam() == null ||
                    apiGame.getHomeTeam().getId() == null || apiGame.getVisitorTeam().getId() == null) {
                    logger.error("Game {} is missing team data. Home: {}, Away: {}", 
                        apiGame.getId(),
                        apiGame.getHomeTeam() != null ? apiGame.getHomeTeam().getId() : "null",
                        apiGame.getVisitorTeam() != null ? apiGame.getVisitorTeam().getId() : "null");
                    return null;
                }
                
                if (existingGame != null) {
                    // Update fields but preserve the ID
                    Games updatedGame = gameMapper.toEntity(apiGame);
                    updatedGame.setId(existingGame.getId());
                    return gamesRepository.save(updatedGame);
                } else {
                    Games newGame = gameMapper.toEntity(apiGame);
                    return gamesRepository.save(newGame);
                }
            })
            .filter(game -> game != null)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<Games> getAndUpdateGamesByYearMonth(int year, int month) {
        logger.debug("Fetching and updating games for {}/{}", year, month);
        List<ApiGame> apiGames = ballDontLieService.getGamesByYearMonth(year, month);
        logger.debug("Received {} games from API", apiGames.size());
        
        return apiGames.stream()
            .map(apiGame -> {
                Games existingGame = gamesRepository
                    .findByExternalId(apiGame.getId())
                    .orElse(null);
                
                if (existingGame != null) {
                    gameMapper.updateEntity(existingGame, apiGame);
                    return gamesRepository.save(existingGame);
                } else {
                    Games newGame = gameMapper.toEntity(apiGame);
                    return gamesRepository.save(newGame);
                }
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void processGameResults(ApiGame apiGame) {
        logger.info("Processing game results for external ID: {}", apiGame.getId());
        
        Games existingGame = gamesRepository.findByExternalId(apiGame.getId())
            .orElseThrow(() -> new RuntimeException("Game not found: " + apiGame.getId()));
        
        logger.info("Found game in DB - ID: {}, Status: {}, Score: {} - {}", 
            existingGame.getId(),
            existingGame.getStatus(),
            existingGame.getHomeTeamScore(),
            existingGame.getAwayTeamScore());
        
        // Update game status and scores
        existingGame.setStatus(apiGame.getStatus());
        existingGame.setHomeTeamScore(apiGame.getHomeTeamScore());
        existingGame.setAwayTeamScore(apiGame.getVisitorTeamScore());
        existingGame.setUpdatedAt(LocalDate.now());
        
        gamesRepository.save(existingGame);
        logger.info("Game updated successfully");
    }
} 