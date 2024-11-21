package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.Players;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GameStatsRepository extends JpaRepository<GameStats, Long> {
    List<GameStats> findByPlayerAndGameGameDateBetween(Players player, LocalDate start, LocalDate end);
    Optional<GameStats> findByPlayerAndGame(Players player, Games game);
    List<GameStats> findByGameIn(List<Games> games);
}