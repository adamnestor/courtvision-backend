package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.Teams;

import java.util.List;
import java.util.Optional;

public interface PlayerService {
    /**
     * Retrieves and updates player information from the API
     *
     * @param playerId The external ID of the player
     * @return Updated player entity
     */
    Players getAndUpdatePlayer(Long playerId);

    /**
     * Retrieves and updates all active players
     *
     * @return List of updated player entities
     */
    List<Players> getAndUpdateActivePlayers();

    /**
     * Retrieves and updates all players for a specific team
     *
     * @param team The team entity
     * @return List of updated player entities
     */
    List<Players> getAndUpdatePlayersByTeam(Teams team);

    /**
     * Finds a player by their external ID
     *
     * @param externalId The BallDontLie API player ID
     * @return Optional containing the player if found
     */
    Optional<Players> findByExternalId(Long externalId);

    /**
     * Searches for players by name
     *
     * @param searchTerm The search term to match against player names
     * @return List of matching players
     */
    List<Players> searchPlayers(String searchTerm);

    /**
     * Gets all active players
     *
     * @return List of active players
     */
    List<Players> getActivePlayers();
} 