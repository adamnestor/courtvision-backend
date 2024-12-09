package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.UserPicks;
import com.adamnestor.courtvision.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserPicksRepository extends JpaRepository<UserPicks, Long> {
    List<UserPicks> findByUser(Users user);
    List<UserPicks> findByUserOrderByCreatedAtDesc(Users user);
    List<UserPicks> findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(
            Users user,
            LocalDateTime start,
            LocalDateTime end
    );
}