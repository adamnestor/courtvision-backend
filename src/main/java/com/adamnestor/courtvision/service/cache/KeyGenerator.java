package com.adamnestor.courtvision.service.cache;

import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.TimePeriod;
import java.time.LocalDate;

public interface KeyGenerator {
    String playerStatsKey(Players player, TimePeriod period);
    String historicalGamesKey(LocalDate date);
    String todaysGamesKey();
} 