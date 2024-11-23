package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.Teams;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TeamsRepository extends JpaRepository<Teams, Long> {
    Optional<Teams> findByExternalId(Long externalId);
    Optional<Teams> findByAbbreviation(String abbreviation);
}