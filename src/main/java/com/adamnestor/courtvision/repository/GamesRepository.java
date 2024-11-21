package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.GameStatus;
import com.adamnestor.courtvision.domain.Games;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GamesRepository extends JpaRepository<Games, Long> {
    Optional<Games> findByExternalId(Long externalId);
    List<Games> findByGameDateBetweenAndStatus(LocalDate start, LocalDate end, GameStatus status);
    List<Games> findBySeasonAndStatus(Integer season, GameStatus status);
}