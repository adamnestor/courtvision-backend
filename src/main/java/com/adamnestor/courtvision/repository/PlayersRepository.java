package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.PlayerStatus;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.Teams;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayersRepository extends JpaRepository<Players, Long> {
    Optional<Players> findByExternalId(Long externalId);
    List<Players> findByStatus(PlayerStatus status);
    List<Players> findByTeam(Teams team);

    @Query("SELECT p FROM Players p WHERE p.team.conference = :conference AND p.status = 'ACTIVE'")
    List<Players> findActivePlayersByConference(@Param("conference") String conference);

    @Query("SELECT p FROM Players p WHERE p.team = :team AND p.status = 'ACTIVE'")
    List<Players> findActivePlayersByTeam(@Param("team") Teams team);
}