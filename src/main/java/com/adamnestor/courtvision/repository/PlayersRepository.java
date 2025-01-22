package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.PlayerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PlayersRepository extends JpaRepository<Players, Long> {
    
    /**
     * Find a player by their external API ID
     */
    Optional<Players> findByExternalId(Long externalId);
    
    /**
     * Find players by first name or last name containing search term (case insensitive)
     */
    List<Players> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName);
    
    /**
     * Find all players with a given status, ordered by last name
     */
    List<Players> findByStatusOrderByLastNameAsc(PlayerStatus status);
    
    /**
     * Find all players on a team
     */
    List<Players> findByTeamId(Long teamId);
    
    /**
     * Find all active players on specified teams
     */
    List<Players> findByTeamIdInAndStatus(Set<Long> teamIds, PlayerStatus status);
    
    List<Players> findByTeamIdIn(Set<Long> teamIds);
}