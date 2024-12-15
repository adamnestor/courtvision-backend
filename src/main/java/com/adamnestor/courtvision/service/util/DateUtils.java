package com.adamnestor.courtvision.service.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class DateUtils {
    private static final ZoneId EASTERN_ZONE = ZoneId.of("America/New_York");

    public LocalDate convertToEasternDate(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(EASTERN_ZONE)
                .toLocalDate();
    }

    public LocalDate getCurrentEasternDate() {
        return ZonedDateTime.now(EASTERN_ZONE).toLocalDate();
    }
}
