package com.adamnestor.courtvision.service.cache;

import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;

@Component("cacheKeyGenerator")
public class CacheKeyGenerator implements KeyGenerator {
    private static final String SEPARATOR = ":";

    // Original manual key generation methods
    public String todaysGamesKey() {
        return buildKey("games", LocalDate.now().toString());
    }

    public String hitRatesKey(Players player, StatCategory category,
                              Integer threshold, TimePeriod period) {
        return buildKey("hitRate",
                player.getId().toString(),
                category.toString(),
                threshold.toString(),
                period.toString());
    }

    public String playerStatsKey(Players player, TimePeriod period) {
        return buildKey("playerStats",
                player.getId().toString(),
                period.toString());
    }

    // Implementation of Spring's KeyGenerator interface
    @Override
    public Object generate(Object target, Method method, Object... params) {
        StringBuilder key = new StringBuilder();

        // Add class and method name
        key.append(target.getClass().getSimpleName())
                .append(SEPARATOR)
                .append(method.getName());

        // Add parameters
        if (params != null) {
            for (Object param : params) {
                key.append(SEPARATOR);
                if (param != null) {
                    if (param instanceof Players player) {
                        key.append("player").append(player.getId());
                    } else if (param instanceof StatCategory category) {
                        key.append(category.name());
                    } else if (param instanceof TimePeriod period) {
                        key.append(period.name());
                    } else {
                        key.append(param.toString());
                    }
                } else {
                    key.append("null");
                }
            }
        }

        return validateKey(key.toString());
    }

    // Helper methods
    private String buildKey(String... parts) {
        return validateKey(String.join(SEPARATOR, parts));
    }

    private String validateKey(String key) {
        return key.toLowerCase()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_:-]", "");
    }
}