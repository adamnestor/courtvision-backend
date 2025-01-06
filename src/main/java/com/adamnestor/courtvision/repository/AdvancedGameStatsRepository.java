package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.AdvancedGameStats;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdvancedGameStatsRepository extends JpaRepository<AdvancedGameStats, Long> {

    Optional<AdvancedGameStats> findByPlayerAndGame(Players player, Games game);

    @Query("SELECT ags FROM AdvancedGameStats ags " +
            "WHERE ags.player = :player " +
            "ORDER BY ags.game.gameDate DESC")
    List<AdvancedGameStats> findPlayerRecentGames(@Param("player") Players player);

    @Query("SELECT ags FROM AdvancedGameStats ags " +
            "WHERE ags.player = :player " +
            "AND ags.game.gameDate BETWEEN :startDate AND :endDate " +
            "ORDER BY ags.game.gameDate DESC")
    List<AdvancedGameStats> findPlayerGamesBetweenDates(
            @Param("player") Players player,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT ags FROM AdvancedGameStats ags " +
            "WHERE ags.player = :player " +
            "AND ags.game.gameDate <= :endDate " +
            "ORDER BY ags.game.gameDate DESC")
    List<AdvancedGameStats> findLastNGames(
            @Param("player") Players player,
            @Param("endDate") LocalDate endDate);
}