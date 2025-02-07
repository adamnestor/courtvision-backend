package com.adamnestor.courtvision.confidence.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class RestCalculator {
    private RestCalculator() {
        throw new IllegalStateException("Utility class");
    }

    public static int calculateDaysOfRest(LocalDate previousGame, LocalDate currentGame) {
        if (previousGame == null || currentGame == null || currentGame.isBefore(previousGame)) {
            return 1; // Default to normal rest if dates are invalid
        }

        return (int) ChronoUnit.DAYS.between(previousGame, currentGame);
    }
}