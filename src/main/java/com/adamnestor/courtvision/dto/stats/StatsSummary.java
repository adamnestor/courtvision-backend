package com.adamnestor.courtvision.dto.stats;

import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import java.math.BigDecimal;

public record StatsSummary(
        StatCategory category,
        Integer threshold,
        TimePeriod timePeriod,
        BigDecimal hitRate,
        BigDecimal average,
        Integer successCount,
        Integer failureCount
) {}