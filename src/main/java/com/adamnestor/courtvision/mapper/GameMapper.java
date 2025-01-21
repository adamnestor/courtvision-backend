package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.api.model.ApiGame;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Teams;
import org.springframework.stereotype.Component;

@Component
public class GameMapper {
    
    private final TeamMapper teamMapper;

    public GameMapper(TeamMapper teamMapper) {
        this.teamMapper = teamMapper;
    }

    public Games toEntity(ApiGame apiGame) {
        if (apiGame == null) {
            return null;
        }

        Games game = new Games();
        game.setExternalId(apiGame.getId());
        game.setGameDate(apiGame.getDate());
        game.setStatus(apiGame.getStatus());
        game.setSeason(apiGame.getSeason());
        game.setGameTime(apiGame.getTime());
        
        // Map teams
        Teams homeTeam = teamMapper.toEntity(apiGame.getHomeTeam());
        Teams awayTeam = teamMapper.toEntity(apiGame.getVisitorTeam());
        game.setHomeTeam(homeTeam);
        game.setAwayTeam(awayTeam);

        return game;
    }

    public void updateEntity(Games existingGame, ApiGame apiGame) {
        if (apiGame == null) {
            return;
        }

        existingGame.setStatus(apiGame.getStatus());
        existingGame.setGameTime(apiGame.getTime());
        
        // Update teams if needed
        Teams homeTeam = teamMapper.toEntity(apiGame.getHomeTeam());
        Teams awayTeam = teamMapper.toEntity(apiGame.getVisitorTeam());
        existingGame.setHomeTeam(homeTeam);
        existingGame.setAwayTeam(awayTeam);
    }
} 