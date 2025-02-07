package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.Games;
import com.adamnestor.courtvision.domain.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GamesRepository extends JpaRepository<Games, Long> {
    Optional<Games> findByExternalId(Long externalId);
    List<Games> findByGameDate(LocalDate date);
    List<Games> findByGameDateAndStatus(LocalDate date, String status);
    List<Games> findByGameDateBetweenAndStatus(LocalDate start, LocalDate end, GameStatus status);
    List<Games> findByGameDateBetween(LocalDate startDate, LocalDate endDate);
}