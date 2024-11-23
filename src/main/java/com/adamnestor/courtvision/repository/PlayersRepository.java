package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.Players;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlayersRepository extends JpaRepository<Players, Long> {
    Optional<Players> findByExternalId(Long externalId);
}