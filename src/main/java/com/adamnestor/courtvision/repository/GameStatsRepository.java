package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Teams;
import com.adamnestor.courtvision.domain.Players;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GameStatsRepository extends JpaRepository<GameStats, Long> {
    // For player detail view - game by game stats
    @Query("SELECT gs FROM GameStats gs " +
            "WHERE gs.player = :player " +
            "ORDER BY gs.game.gameDate DESC")
    List<GameStats> findPlayerRecentGames(@Param("player") Players player);

    // For calculating hit rates
    @Query("SELECT COUNT(gs) * 100.0 / COUNT(*) FROM GameStats gs " +
            "WHERE gs.player = :player " +
            "AND gs.game.gameDate BETWEEN :startDate AND :endDate " +
            "AND gs.points >= :threshold")
    Double calculatePointsHitRate(
            @Param("player") Players player,
            @Param("threshold") Integer threshold,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COUNT(gs) * 100.0 / COUNT(*) FROM GameStats gs " +
            "WHERE gs.player = :player " +
            "AND gs.game.gameDate BETWEEN :startDate AND :endDate " +
            "AND gs.assists >= :threshold")
    Double calculateAssistsHitRate(
            @Param("player") Players player,
            @Param("threshold") Integer threshold,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COUNT(gs) * 100.0 / COUNT(*) FROM GameStats gs " +
            "WHERE gs.player = :player " +
            "AND gs.game.gameDate BETWEEN :startDate AND :endDate " +
            "AND gs.rebounds >= :threshold")
    Double calculateReboundsHitRate(
            @Param("player") Players player,
            @Param("threshold") Integer threshold,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // For calculating averages
    @Query("SELECT AVG(gs.points) FROM GameStats gs " +
            "WHERE gs.player = :player " +
            "AND gs.game.gameDate BETWEEN :startDate AND :endDate")
    Double calculatePointsAverage(
            @Param("player") Players player,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Optional<GameStats> findByPlayerAndGame(Players player, Games game);

    @Query("SELECT gs FROM GameStats gs " +
            "WHERE gs.player = :player " +
            "AND gs.game.gameDate < :date " +
            "ORDER BY gs.game.gameDate DESC " +
            "LIMIT 1")
    Optional<GameStats> findPreviousGame(
            @Param("player") Players player,
            @Param("date") LocalDate date);

    @Query("SELECT gs FROM GameStats gs " +
            "WHERE gs.player = :player " +
            "AND gs.game.gameDate BETWEEN :start AND :end " +
            "ORDER BY gs.game.gameDate ASC")
    List<GameStats> findGamesByDateRange(
            @Param("player") Players player,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("SELECT gs FROM GameStats gs " +
            "WHERE gs.player = :player " +
            "AND ABS(gs.game.homeTeamScore - gs.game.awayTeamScore) >= :threshold " +
            "AND gs.game.gameDate BETWEEN :startDate AND :endDate " +
            "ORDER BY gs.game.gameDate DESC")
    List<GameStats> findBlowoutGames(
            @Param("player") Players player,
            @Param("threshold") Integer threshold,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<Games> findGamesByTeams(Teams team1, Teams team2, LocalDate sinceDate);




}