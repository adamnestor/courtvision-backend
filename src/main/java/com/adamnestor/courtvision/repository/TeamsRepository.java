package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.Teams;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TeamsRepository extends JpaRepository<Teams, Long> {
    Optional<Teams> findByExternalId(Long externalId);
    Optional<Teams> findByAbbreviation(String abbreviation);
}