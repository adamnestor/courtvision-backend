package com.adamnestor.courtvision.confidence.service.impl;

import com.adamnestor.courtvision.confidence.model.RestImpact;
import com.adamnestor.courtvision.confidence.service.RestImpactService;
import com.adamnestor.courtvision.confidence.util.RestCalculator;
import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class RestImpactServiceImpl implements RestImpactService {
    private final GameStatsRepository gameStatsRepository;

    public RestImpactServiceImpl(GameStatsRepository gameStatsRepository) {
        this.gameStatsRepository = gameStatsRepository;
    }

    @Override
    public RestImpact calculateRestImpact(Players player, Games currentGame) {
        Optional<GameStats> previousGame = gameStatsRepository.findPreviousGame(
                player, currentGame.getGameDate());

        int daysOfRest = previousGame
                .map(stats -> RestCalculator.calculateDaysOfRest(
                        stats.getGame().getGameDate(),
                        currentGame.getGameDate()))
                .orElse(1); // Default to normal rest if no previous game

        return new RestImpact(daysOfRest);
    }
}