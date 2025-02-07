package com.adamnestor.courtvision.confidence.service;

import com.adamnestor.courtvision.domain.*;
import java.math.BigDecimal;

public interface ConfidenceScoreService {
    BigDecimal calculateConfidenceScore(
            Players player,
            Games game,
            StatCategory category,
            Integer threshold,
            BigDecimal hitRate,
            int gamesCount
    );
}
