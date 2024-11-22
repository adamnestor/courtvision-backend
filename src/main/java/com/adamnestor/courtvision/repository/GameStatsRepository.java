package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GameStatsRepository extends JpaRepository<GameStats, Long> {
    List<GameStats> findByPlayerAndGameGameDateBetween(Players player, LocalDate start, LocalDate end);
    Optional<GameStats> findByPlayerAndGame(Players player, Games game);
    List<GameStats> findByGameIn(List<Games> games);

    @Query("SELECT gs FROM GameStats gs WHERE gs.player = :player AND gs.points >= :threshold")
    List<GameStats> findByPlayerAndPointsThreshold(
            @Param("player") Players player,
            @Param("threshold") Integer threshold);

    @Query("SELECT AVG(gs.points) FROM GameStats gs WHERE gs.player = :player AND gs.game.gameDate BETWEEN :startDate AND :endDate")
    Double calculateAveragePointsInDateRange(
            @Param("player") Players player,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(gs) * 100.0 / (SELECT COUNT(gs2) FROM GameStats gs2 WHERE gs2.player = :player AND gs2.game.gameDate BETWEEN :startDate AND :endDate) " +
            "FROM GameStats gs WHERE gs.player = :player AND gs.points >= :threshold AND gs.game.gameDate BETWEEN :startDate AND :endDate")
    Double calculateHitRateForPoints(
            @Param("player") Players player,
            @Param("threshold") Integer threshold,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(gs) * 100.0 / (SELECT COUNT(gs2) FROM GameStats gs2 WHERE gs2.player = :player AND gs2.game.gameDate BETWEEN :startDate AND :endDate) " +
            "FROM GameStats gs WHERE gs.player = :player AND gs.assists >= :threshold AND gs.game.gameDate BETWEEN :startDate AND :endDate")
    Double calculateHitRateForAssists(
            @Param("player") Players player,
            @Param("threshold") Integer threshold,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(gs) * 100.0 / (SELECT COUNT(gs2) FROM GameStats gs2 WHERE gs2.player = :player AND gs2.game.gameDate BETWEEN :startDate AND :endDate) " +
            "FROM GameStats gs WHERE gs.player = :player AND gs.rebounds >= :threshold AND gs.game.gameDate BETWEEN :startDate AND :endDate")
    Double calculateHitRateForRebounds(
            @Param("player") Players player,
            @Param("threshold") Integer threshold,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}