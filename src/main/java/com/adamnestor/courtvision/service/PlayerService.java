package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.api.model.ApiPlayer;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.Teams;
import com.adamnestor.courtvision.domain.PlayerStatus;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.mapper.PlayerMapper;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.repository.TeamsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.stream.Stream;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PlayerService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerService.class);

    private final BallDontLieService ballDontLieService;
    private final PlayersRepository playersRepository;
    private final PlayerMapper playerMapper;
    private final GameService gameService;
    private final TeamsRepository teamsRepository;

    public PlayerService(
            BallDontLieService ballDontLieService,
            PlayersRepository playersRepository,
            PlayerMapper playerMapper,
            GameService gameService,
            TeamsRepository teamsRepository) {
        this.ballDontLieService = ballDontLieService;
        this.playersRepository = playersRepository;
        this.playerMapper = playerMapper;
        this.gameService = gameService;
        this.teamsRepository = teamsRepository;
    }

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

    @Transactional
    public List<Players> getAndUpdateActivePlayers() {
        logger.debug("Fetching and updating current roster players from /players/active");
        List<ApiPlayer> apiPlayers = ballDontLieService.getAllPlayers();

        if (apiPlayers.isEmpty()) {
            logger.error("No players returned from /players/active endpoint");
            throw new RuntimeException("No active players found");
        }

        AtomicInteger processedCount = new AtomicInteger(0);

        return apiPlayers.stream()
                .map(apiPlayer -> {
                    try {
                        // Add small delay every 5 players
                        if (processedCount.incrementAndGet() % 5 == 0) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }

                        // Find or create player
                        Players player = playersRepository.findByExternalId(apiPlayer.getId())
                                .orElseGet(() -> new Players());

                        // Update player info
                        playerMapper.updateEntity(player, apiPlayer);

                        // Update team if provided
                        if (apiPlayer.getTeam() != null) {
                            Teams team = teamsRepository.findByExternalId(apiPlayer.getTeam().getId())
                                    .orElse(null);
                            player.setTeam(team);
                        }

                        return playersRepository.save(player);
                    } catch (Exception e) {
                        logger.error("Error updating player {} {}: {}",
                                apiPlayer.getFirstName(), apiPlayer.getLastName(), e.getMessage());
                        throw e;
                    }
                })
                .collect(Collectors.toList());
    }

    @Retryable(
            value = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    @Transactional
    public List<Players> getAndUpdatePlayersByTeam(Teams team) {
        logger.info("Starting player update for team: {} (ID: {}, ExternalID: {})",
                team.getName(), team.getId(), team.getExternalId());

        List<ApiPlayer> apiPlayers = ballDontLieService.getPlayersByTeam(team.getExternalId());
        logger.info("Retrieved {} players from API for team {}", apiPlayers.size(), team.getName());

        Set<Long> updatedPlayerIds = new HashSet<>();
        AtomicInteger processedCount = new AtomicInteger(0);

        List<Players> updatedPlayers = apiPlayers.stream()
                .map(apiPlayer -> {
                    try {
                        // Add small delay every 5 players to ensure clean DB writes
                        if (processedCount.incrementAndGet() % 5 == 0) {
                            try {
                                Thread.sleep(100); // 100ms pause every 5 players
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }

                        logger.debug("Processing player {} {} (API ID: {})",
                                apiPlayer.getFirstName(),
                                apiPlayer.getLastName(),
                                apiPlayer.getId());

                        Players existingPlayer = playersRepository.findByExternalId(apiPlayer.getId())
                                .orElse(null);

                        Players result;
                        if (existingPlayer != null) {
                            playerMapper.updateEntity(existingPlayer, apiPlayer);
                            existingPlayer.setTeam(team);
                            updatedPlayerIds.add(existingPlayer.getId());
                            result = playersRepository.save(existingPlayer);
                            logger.debug("Updated existing player: {} {} (ID: {})",
                                    result.getFirstName(), result.getLastName(), result.getId());
                        } else {
                            Players newPlayer = playerMapper.toEntity(apiPlayer);
                            newPlayer.setTeam(team);
                            result = playersRepository.save(newPlayer);
                            logger.debug("Created new player: {} {} (ID: {})",
                                    result.getFirstName(), result.getLastName(), result.getId());
                        }
                        return result;
                    } catch (Exception e) {
                        logger.error("Error processing player from API: {} {}: {}",
                                apiPlayer.getFirstName(), apiPlayer.getLastName(), e.getMessage());
                        throw e;  // Retry will handle this
                    }
                })
                .collect(Collectors.toList());

        // Handle former team players in a separate transaction
        handleFormerTeamPlayers(team, updatedPlayerIds);

        logger.info("Completed player update for team {}. Updated {} players",
                team.getName(), updatedPlayers.size());
        return updatedPlayers;
    }

    @Transactional
    private void handleFormerTeamPlayers(Teams team, Set<Long> updatedPlayerIds) {
        List<Players> formerTeamPlayers = playersRepository.findByTeamId(team.getId());
        int removedCount = 0;

        for (Players player : formerTeamPlayers) {
            if (!updatedPlayerIds.contains(player.getId())) {
                logger.info("Removing team association for player {} {} (ID: {})",
                        player.getFirstName(), player.getLastName(), player.getId());
                player.setTeam(null);
                playersRepository.save(player);
                removedCount++;
            }
        }

        if (removedCount > 0) {
            logger.info("Removed team association for {} players from team {}",
                    removedCount, team.getName());
        }
    }

    public Optional<Players> findByExternalId(Long externalId) {
        return playersRepository.findByExternalId(externalId);
    }

    public List<Players> searchPlayers(String searchTerm) {
        return playersRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                searchTerm, searchTerm);
    }

    public List<Players> getActivePlayers() {
        return playersRepository.findByStatusOrderByLastNameAsc(PlayerStatus.ACTIVE);
    }

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

    public List<Players> getAllPlayers() {
        return playersRepository.findAll();
    }

    public Players updatePlayer(Players player) {
        return playersRepository.save(player);
    }
}