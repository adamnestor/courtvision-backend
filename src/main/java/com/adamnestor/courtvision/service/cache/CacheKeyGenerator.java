package com.adamnestor.courtvision.service.cache;

import com.adamnestor.courtvision.config.CacheConfig;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Generates consistent cache keys for all cached entities
 */
@Component
public class CacheKeyGenerator {
    private static final String SEPARATOR = ":";

    /**
     * Generates a cache key for today's games
     */
    public String todaysGamesKey() {
        return buildKey(CacheConfig.GAMES_PREFIX, LocalDate.now().toString());
    }

    /**
     * Generates a cache key for player hit rates
     */
    public String playerHitRatesKey(Players player, StatCategory category,
                                    Integer threshold, TimePeriod period) {
        return buildKey(
                CacheConfig.HIT_RATE_PREFIX,
                player.getId().toString(),
                category.toString(),
                threshold.toString(),
                period.toString()
        );
    }

    /**
     * Generates a cache key for player stats
     */
    public String playerStatsKey(Players player, TimePeriod period) {
        return buildKey(
                CacheConfig.STATS_PREFIX,
                player.getId().toString(),
                period.toString()
        );
    }

    /**
     * Builds a cache key from parts
     */
    private String buildKey(String... parts) {
        Objects.requireNonNull(parts, "Key parts cannot be null");
        if (parts.length == 0) {
            throw new IllegalArgumentException("At least one key part is required");
        }

        return String.join(SEPARATOR, parts)
                .replaceAll("\\s+", "_")
                .toLowerCase();
    }

    /**
     * Validates a cache key
     */
    public String validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Cache key cannot be null or empty");
        }

        // Remove any invalid characters and standardize format
        return key.trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9_:-]", "")
                .toLowerCase();
    }
}