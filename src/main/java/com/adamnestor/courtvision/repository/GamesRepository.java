package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.GameStatus;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Teams;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GamesRepository extends JpaRepository<Games, Long> {
    Optional<Games> findByExternalId(Long externalId);
    List<Games> findByGameDateBetweenAndStatus(LocalDate start, LocalDate end, GameStatus status);
    List<Games> findBySeasonAndStatus(Integer season, GameStatus status);

    @Query("SELECT g FROM Games g WHERE g.gameDate >= :date AND g.status = 'SCHEDULED'")
    List<Games> findUpcomingGames(@Param("date") LocalDate date);

    @Query("SELECT g FROM Games g WHERE (g.homeTeam = :team OR g.awayTeam = :team) AND g.gameDate >= :date")
    List<Games> findTeamSchedule(@Param("team") Teams team, @Param("date") LocalDate date);
}