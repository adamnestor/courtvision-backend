package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.UserPicks;
import com.adamnestor.courtvision.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserPicksRepository extends JpaRepository<UserPicks, Long> {
    List<UserPicks> findByUser(Users user);

    List<UserPicks> findByUserOrderByCreatedAtDesc(Users user);

    @Query("SELECT p FROM UserPicks p WHERE p.user = :user AND p.createdAt BETWEEN :start AND :end ORDER BY p.createdAt DESC")
    List<UserPicks> findByUserAndDateRange(
            @Param("user") Users user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<UserPicks> findByUserAndParlayId(Users user, String parlayId);

    @Query("SELECT DISTINCT p.parlayId FROM UserPicks p WHERE p.user = :user AND p.parlayId IS NOT NULL")
    List<String> findDistinctParlayIds(@Param("user") Users user);

    @Modifying
    @Query("DELETE FROM UserPicks p WHERE p.user = :user AND p.parlayId = :parlayId")
    void deleteByUserAndParlayId(@Param("user") Users user, @Param("parlayId") String parlayId);

    @Query("SELECT p FROM UserPicks p WHERE p.game.gameDate = :date")
    List<UserPicks> findPicksByGameDate(@Param("date") LocalDate date);

    List<UserPicks> findByParlayId(String parlayId);

    @Query("SELECT p FROM UserPicks p WHERE p.user = :user AND p.result IS NOT NULL")
    List<UserPicks> findCompletedPicksByUser(@Param("user") Users user);
}