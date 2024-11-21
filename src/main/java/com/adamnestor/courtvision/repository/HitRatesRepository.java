package com.adamnestor.courtvision.repository;

import com.adamnestor.courtvision.domain.HitRates;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HitRatesRepository extends JpaRepository<HitRates, Long> {
    List<HitRates> findByPlayerAndCategoryAndTimePeriod(Players player, StatCategory category, TimePeriod timePeriod);
    void deleteByPlayerAndCategoryAndTimePeriod(Players player, StatCategory category, TimePeriod timePeriod);
    List<HitRates> findByPlayerAndTimePeriod(Players player, TimePeriod timePeriod);
}