package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.HitRates;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface HitRatesRepository extends JpaRepository<HitRates, Long> {
    // For dashboard display
    @Query("SELECT h FROM HitRates h " +
            "WHERE h.timePeriod = :period " +
            "AND h.category = :category " +
            "AND h.threshold = :threshold " +
            "ORDER BY h.hitRate DESC")
    List<HitRates> findTopHitRates(
            @Param("period") TimePeriod period,
            @Param("category") StatCategory category,
            @Param("threshold") Integer threshold
    );

    // For player detail view
    List<HitRates> findByPlayerAndCategoryAndTimePeriodOrderByHitRateDesc(
            Players player,
            StatCategory category,
            TimePeriod timePeriod
    );
}