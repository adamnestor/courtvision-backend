package com.adamnestor.courtvision.service.impl;

import com.adamnestor.courtvision.api.model.ApiPlayer;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.Teams;
import com.adamnestor.courtvision.domain.PlayerStatus;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.mapper.PlayerMapper;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.service.BallDontLieService;
import com.adamnestor.courtvision.service.PlayerService;
import com.adamnestor.courtvision.service.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class PlayerServiceImpl implements PlayerService {
    
    private static final Logger logger = LoggerFactory.getLogger(PlayerServiceImpl.class);
    
    private final BallDontLieService ballDontLieService;
    private final PlayersRepository playersRepository;
    private final PlayerMapper playerMapper;
    private final GameService gameService;

    public PlayerServiceImpl(
            BallDontLieService ballDontLieService,
            PlayersRepository playersRepository,
            PlayerMapper playerMapper,
            GameService gameService) {
        this.ballDontLieService = ballDontLieService;
        this.playersRepository = playersRepository;
        this.playerMapper = playerMapper;
        this.gameService = gameService;
    }

    @Override
    @Transactional
    public Players getAndUpdatePlayer(Long playerId) {
        logger.debug("Fetching and updating player with ID: {}", playerId);
        
        Players existingPlayer = playersRepository.findByExternalId(playerId).orElse(null);
        if (existingPlayer != null) {
            ApiPlayer apiPlayer = ballDontLieService.getPlayer(playerId);
            if (apiPlayer == null || apiPlayer.getId() == null) {
                logger.error("Failed to fetch player data for ID: {}", playerId);
                return existingPlayer;  // Return existing data rather than updating with null
            }
            playerMapper.updateEntity(existingPlayer, apiPlayer);
            return playersRepository.save(existingPlayer);
        } else {
            ApiPlayer apiPlayer = ballDontLieService.getPlayer(playerId);
            if (apiPlayer == null || apiPlayer.getId() == null) {
                logger.error("Failed to fetch player data for ID: {}", playerId);
                return null;
            }
            Players newPlayer = playerMapper.toEntity(apiPlayer);
            return playersRepository.save(newPlayer);
        }
    }

    @Override
    @Transactional
    public List<Players> getAndUpdateActivePlayers() {
        logger.debug("Fetching and updating all active players");
        List<ApiPlayer> apiPlayers = ballDontLieService.getAllPlayers();
        
        return apiPlayers.stream()
            .map(apiPlayer -> {
                Players existingPlayer = playersRepository.findByExternalId(apiPlayer.getId())
                    .orElse(null);
                    
                if (existingPlayer != null) {
                    playerMapper.updateEntity(existingPlayer, apiPlayer);
                    return playersRepository.save(existingPlayer);
                } else {
                    Players newPlayer = playerMapper.toEntity(apiPlayer);
                    return playersRepository.save(newPlayer);
                }
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<Players> getAndUpdatePlayersByTeam(Teams team) {
        logger.debug("Fetching and updating players for team: {}", team.getName());
        List<ApiPlayer> apiPlayers = ballDontLieService.getPlayersByTeam(team.getExternalId());
        
        return apiPlayers.stream()
            .map(apiPlayer -> {
                Players existingPlayer = playersRepository.findByExternalId(apiPlayer.getId())
                    .orElse(null);
                    
                if (existingPlayer != null) {
                    playerMapper.updateEntity(existingPlayer, apiPlayer);
                    return playersRepository.save(existingPlayer);
                } else {
                    Players newPlayer = playerMapper.toEntity(apiPlayer);
                    return playersRepository.save(newPlayer);
                }
            })
            .collect(Collectors.toList());
    }

    @Override
    public Optional<Players> findByExternalId(Long externalId) {
        return playersRepository.findByExternalId(externalId);
    }

    @Override
    public List<Players> searchPlayers(String searchTerm) {
        return playersRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            searchTerm, searchTerm);
    }

    @Override
    public List<Players> getActivePlayers() {
        return playersRepository.findByStatusOrderByLastNameAsc(PlayerStatus.ACTIVE);
    }

    @Override
    public List<Players> getPlayersWithGameToday() {
        List<Games> todaysGames = gameService.getTodaysGames();
        Set<Long> teamsWithGames = todaysGames.stream()
            .flatMap(game -> Stream.of(
                game.getHomeTeam().getId(), 
                game.getAwayTeam().getId()
            ))
            .collect(Collectors.toSet());
        
        return playersRepository.findByTeamIdIn(teamsWithGames);
    }
} 