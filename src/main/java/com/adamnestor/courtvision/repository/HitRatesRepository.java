package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.HitRates;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface HitRatesRepository extends JpaRepository<HitRates, Long> {
    List<HitRates> findByPlayerAndCategoryAndTimePeriod(Players player, StatCategory category, TimePeriod timePeriod);
    void deleteByPlayerAndCategoryAndTimePeriod(Players player, StatCategory category, TimePeriod timePeriod);
    List<HitRates> findByPlayerAndTimePeriod(Players player, TimePeriod timePeriod);

    @Query("SELECT h FROM HitRates h WHERE h.hitRate >= :threshold AND h.timePeriod = :timePeriod ORDER BY h.hitRate DESC")
    List<HitRates> findTopHitRates(@Param("threshold") BigDecimal threshold, @Param("timePeriod") TimePeriod timePeriod);

    @Query("SELECT h FROM HitRates h WHERE h.category = :category AND h.timePeriod = :timePeriod AND h.hitRate >= :minRate ORDER BY h.hitRate DESC")
    List<HitRates> findTopHitRatesByCategory(
            @Param("category") StatCategory category,
            @Param("timePeriod") TimePeriod timePeriod,
            @Param("minRate") BigDecimal minRate);
}