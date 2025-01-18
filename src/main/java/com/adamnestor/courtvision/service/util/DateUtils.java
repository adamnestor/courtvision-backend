package com.adamnestor.courtvision.service.util;

import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class DateUtils {
    private static final ZoneId EASTERN_ZONE = ZoneId.of("America/New_York");
    
    public LocalDate getCurrentEasternDate() {
        return LocalDate.now(EASTERN_ZONE);
    }
}
