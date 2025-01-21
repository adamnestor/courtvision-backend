package com.adamnestor.courtvision.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for standardizing API response formatting.
 */
public class ResponseUtils {
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public static String formatGameResult(Boolean result) {
        if (result == null) return null;
        return result ? "WIN" : "LOSS";
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(ISO_FORMATTER);
    }

    public static String formatDateTime(LocalDate date) {
        return date.atStartOfDay().format(ISO_FORMATTER);
    }

    public static boolean isHighConfidence(int confidenceScore) {
        return confidenceScore >= 80;
    }
} 