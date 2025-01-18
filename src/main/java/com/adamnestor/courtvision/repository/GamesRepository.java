package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface GamesRepository extends JpaRepository<Games, Long> {
    List<Games> findByGameDate(LocalDate date);
    List<Games> findByGameDateAndStatus(LocalDate date, GameStatus status);
    List<Games> findByGameDateBetweenAndStatus(LocalDate start, LocalDate end, GameStatus status);
}