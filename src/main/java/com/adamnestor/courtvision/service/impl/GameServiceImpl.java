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
    public Games findByExternalId(Long externalId) {
        return gamesRepository.findByExternalId(externalId)
            .orElse(null);
    }

    @Override
    public List<Games> getTodaysGames() {
        return gamesRepository.findByGameDate(LocalDate.now());
    }

    @Override
    public List<Games> getGamesByDateRange(LocalDate startDate, LocalDate endDate) {
        return gamesRepository.findByGameDateBetween(startDate, endDate);
    }
} 