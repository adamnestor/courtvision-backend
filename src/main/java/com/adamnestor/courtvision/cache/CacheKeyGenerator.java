package com.adamnestor.courtvision.cache;

import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class CacheKeyGenerator {
    private static final Logger logger = LoggerFactory.getLogger(CacheKeyGenerator.class);
    private static final String VERSION = "v1";
    private static final String DELIMITER = ":";
    
    private String addVersion(String key) {
        return VERSION + DELIMITER + key;
    }
    
    // Player-related keys
    public String generatePlayerKey(Players player, StatCategory category, Integer threshold, TimePeriod period) {
        validatePlayer(player);
        String key = String.join(DELIMITER, 
            player.getId().toString(),
            category.toString(),
            threshold.toString(),
            period.toString()
        );
        String versionedKey = addVersion(key);
        logKeyGeneration("Player Stats", versionedKey);
        return versionedKey;
    }
    
    // Game-related keys
    public String generateGameKey(Long gameId, String type) {
        validateGameId(gameId);
        String key = String.join(DELIMITER, gameId.toString(), type);
        String versionedKey = addVersion(key);
        logKeyGeneration("Game Stats", versionedKey);
        return versionedKey;
    }
    
    // Confidence score keys
    public String generateConfidenceKey(Players player, StatCategory category, Integer threshold) {
        validatePlayer(player);
        String key = String.join(DELIMITER,
            "confidence",
            player.getId().toString(),
            category.toString(),
            threshold.toString()
        );
        String versionedKey = addVersion(key);
        logKeyGeneration("Confidence Score", versionedKey);
        return versionedKey;
    }
    
    // Dashboard stats keys
    public String generateDashboardKey(TimePeriod period, StatCategory category, 
                                     Integer threshold, String sortBy, String sortDirection) {
        if (period == null || category == null || threshold == null || 
            sortBy == null || sortDirection == null) {
            logger.error("Invalid dashboard parameters for cache key generation");
            throw new IllegalArgumentException("All dashboard parameters must be non-null");
        }
        
        String key = String.join(DELIMITER,
            period.toString(),
            category.toString(),
            threshold.toString(),
            sortBy,
            sortDirection
        );
        String versionedKey = addVersion(key);
        logKeyGeneration("Dashboard Stats", versionedKey);
        return versionedKey;
    }
    
    // Collection keys (for lists of items)
    public String generateCollectionKey(String prefix, List<?> items) {
        String hashCode = String.valueOf(Objects.hash(items.stream()
            .map(Object::toString)
            .collect(Collectors.joining())));
        String key = prefix + DELIMITER + hashCode;
        String versionedKey = addVersion(key);
        logKeyGeneration("Collection", versionedKey);
        return versionedKey;
    }
    
    // API response keys
    public String generateApiKey(String endpoint, Object... params) {
        if (endpoint == null) {
            logger.error("Invalid API endpoint for cache key generation");
            throw new IllegalArgumentException("API endpoint cannot be null");
        }
        
        String paramString = params == null ? "" : 
            Arrays.stream(params)
                .map(Object::toString)
                .collect(Collectors.joining(DELIMITER));
                
        String key = endpoint + (paramString.isEmpty() ? "" : DELIMITER + paramString);
        String versionedKey = addVersion(key);
        logKeyGeneration("API Response", versionedKey);
        return versionedKey;
    }
    
    private void validatePlayer(Players player) {
        if (player == null || player.getId() == null) {
            logger.error("Invalid player data for cache key generation");
            throw new IllegalArgumentException("Player and player ID cannot be null");
        }
    }
    
    private void validateGameId(Long gameId) {
        if (gameId == null || gameId <= 0) {
            logger.error("Invalid game ID for cache key generation");
            throw new IllegalArgumentException("Game ID must be positive");
        }
    }
    
    private void logKeyGeneration(String type, String key) {
        logger.debug("Generated {} cache key: {}", type, key);
    }
} 