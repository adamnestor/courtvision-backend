package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.AdvancedGameStats;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.Teams;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdvancedGameStatsRepository extends JpaRepository<AdvancedGameStats, Long> {

    // Basic finder methods
    Optional<AdvancedGameStats> findByPlayerAndGame(Players player, Games game);

    // Find most recent advanced stats for a player
    @Query("SELECT ags FROM AdvancedGameStats ags " +
            "WHERE ags.player = :player " +
            "ORDER BY ags.game.gameDate DESC")
    List<AdvancedGameStats> findPlayerRecentGames(@Param("player") Players player);

    // Find games in date range with optional player filter
    @Query("SELECT ags FROM AdvancedGameStats ags " +
            "WHERE (:player IS NULL OR ags.player = :player) " +
            "AND ags.game.gameDate BETWEEN :startDate AND :endDate " +
            "ORDER BY ags.game.gameDate DESC")
    List<AdvancedGameStats> findGamesByDateRange(
            @Param("player") Players player,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Find team's games in date range
    @Query("SELECT ags FROM AdvancedGameStats ags " +
            "WHERE (ags.game.homeTeam = :team OR ags.game.awayTeam = :team) " +
            "AND ags.game.gameDate BETWEEN :startDate AND :endDate " +
            "ORDER BY ags.game.gameDate DESC")
    List<AdvancedGameStats> findTeamGamesByDateRange(
            @Param("team") Teams team,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Team average pace
    @Query("SELECT AVG(ags.pace) FROM AdvancedGameStats ags " +
            "WHERE (ags.game.homeTeam = :team OR ags.game.awayTeam = :team) " +
            "AND ags.game.gameDate BETWEEN :startDate AND :endDate")
    Optional<Double> findTeamAveragePace(
            @Param("team") Teams team,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Team average defensive rating
    @Query("SELECT AVG(ags.defensiveRating) FROM AdvancedGameStats ags " +
            "WHERE (ags.game.homeTeam = :team OR ags.game.awayTeam = :team) " +
            "AND ags.game.gameDate BETWEEN :startDate AND :endDate")
    Optional<Double> findTeamAverageDefensiveRating(
            @Param("team") Teams team,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Find last N games for a player
    @Query(value = "SELECT ags FROM AdvancedGameStats ags " +
            "WHERE ags.player = :player " +
            "ORDER BY ags.game.gameDate DESC " +
            "LIMIT :limit")
    List<AdvancedGameStats> findLastNGames(
            @Param("player") Players player,
            @Param("limit") int limit);

    // Check if stats exist for a game
    boolean existsByGame(Games game);

    @Query("SELECT AVG(ags.netRating) FROM AdvancedGameStats ags " +
            "WHERE (ags.game.homeTeam = :team OR ags.game.awayTeam = :team) " +
            "AND ags.game.gameDate BETWEEN :startDate AND :endDate")
    Optional<BigDecimal> findTeamAverageNetRating(
            @Param("team") Teams team,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}