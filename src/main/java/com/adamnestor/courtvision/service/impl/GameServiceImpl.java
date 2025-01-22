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
                logger.debug("Mapping API game: {} with teams: visitor={}, home={}", 
                    apiGame.getId(), 
                    apiGame.getVisitorTeam(), 
                    apiGame.getHomeTeam());
                Games existingGame = gamesRepository.findByExternalId(apiGame.getId())
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
    public List<Games> getAndUpdateGamesBySeason(Integer season) {
        logger.debug("Fetching and updating games for season: {}", season);
        List<ApiGame> apiGames = ballDontLieService.getGamesBySeason(season);
        
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
    public List<Games> getGamesByDateRange(LocalDate startDate, LocalDate endDate) {
        return gamesRepository.findByGameDateBetween(startDate, endDate);
    }
} 