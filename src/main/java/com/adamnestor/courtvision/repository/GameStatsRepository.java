package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Teams;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.PlayerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
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

    @Query("SELECT DISTINCT gs.game FROM GameStats gs " +
           "WHERE (gs.game.homeTeam = :team1 AND gs.game.awayTeam = :team2 " +
           "    OR gs.game.homeTeam = :team2 AND gs.game.awayTeam = :team1) " +
           "AND gs.game.gameDate >= :sinceDate " +
           "ORDER BY gs.game.gameDate DESC")
    List<Games> findGamesByTeams(
            @Param("team1") Teams team1, 
            @Param("team2") Teams team2, 
            @Param("sinceDate") LocalDate sinceDate
    );

    @Query("SELECT AVG(gs.assists) FROM GameStats gs " +
            "WHERE gs.player = :player " +
            "AND gs.game.gameDate BETWEEN :startDate AND :endDate")
    Double calculateAssistsAverage(
            @Param("player") Players player,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT AVG(gs.rebounds) FROM GameStats gs " +
            "WHERE gs.player = :player " +
            "AND gs.game.gameDate BETWEEN :startDate AND :endDate")
    Double calculateReboundsAverage(
            @Param("player") Players player,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find stats by game and player
     */
    Optional<GameStats> findByGameAndPlayer(Games game, Players player);

    /**
     * Find stats by external ID
     */
    Optional<GameStats> findByExternalId(Long externalId);

    /**
     * Find game by external ID
     */
    @Query("SELECT gs.game FROM GameStats gs WHERE gs.externalId = :externalId")
    Optional<Games> findGameByExternalId(Long externalId);

    /**
     * Find recent stats for a player, ordered by game date
     */
    @Query(value = "SELECT gs FROM GameStats gs " +
           "WHERE gs.player = :player " +
           "ORDER BY gs.game.gameDate DESC " +
           "LIMIT :limit")
    List<GameStats> findByPlayerOrderByGameDateDesc(@Param("player") Players player, @Param("limit") int limit);

    /**
     * Find all stats for a game
     */
    List<GameStats> findByGame(Games game);

    @Query("""
        SELECT gs FROM GameStats gs 
        WHERE gs.player.team IN :teams 
        AND gs.player.status = :status
        AND gs.game.gameDate < :today
        ORDER BY gs.game.gameDate DESC
        """)
    List<GameStats> findRecentStatsByTeamsAndCategory(
        @Param("teams") Set<Teams> teams,
        @Param("status") PlayerStatus status,
        @Param("today") LocalDate today
    );
}