package com.adamnestor.courtvision.test.cache;

import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.domain.StatCategory;
import com.adamnestor.courtvision.domain.TimePeriod;
import com.adamnestor.courtvision.service.cache.CacheKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CacheKeyGeneratorTest {

    private CacheKeyGenerator keyGenerator;

    private Players testPlayer;

    @BeforeEach
    void setUp() {
        keyGenerator = new CacheKeyGenerator();

        testPlayer = new Players();
        testPlayer.setId(1L);
        testPlayer.setFirstName("Test");
        testPlayer.setLastName("Player");
    }

    @Test
    void todaysGamesKey_ShouldIncludeCurrentDate() {
        // When
        String key = keyGenerator.todaysGamesKey();

        // Then
        assertNotNull(key);
        assertTrue(key.startsWith("games:"));
        assertTrue(key.contains(LocalDate.now().toString()));
    }

    @Test
    void playerHitRatesKey_ShouldIncludeAllComponents() {
        // Given
        StatCategory category = StatCategory.POINTS;
        Integer threshold = 15;
        TimePeriod period = TimePeriod.L10;

        // When
        String key = keyGenerator.playerHitRatesKey(testPlayer, category, threshold, period);

        // Then
        assertNotNull(key);
        assertTrue(key.contains("hitrate"));
        assertTrue(key.contains(testPlayer.getId().toString()));
        assertTrue(key.contains(category.toString().toLowerCase()));
        assertTrue(key.contains(threshold.toString()));
        assertTrue(key.contains(period.toString().toLowerCase()));
    }

    @Test
    void playerStatsKey_ShouldIncludePlayerAndPeriod() {
        // Given
        TimePeriod period = TimePeriod.L10;

        // When
        String key = keyGenerator.playerStatsKey(testPlayer, period);

        // Then
        assertNotNull(key);
        assertTrue(key.contains("stats"));
        assertTrue(key.contains(testPlayer.getId().toString()));
        assertTrue(key.contains(period.toString().toLowerCase()));
    }

    @Test
    void validateKey_ShouldHandleNullInput() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                keyGenerator.validateKey(null)
        );
    }

    @Test
    void validateKey_ShouldHandleEmptyInput() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                keyGenerator.validateKey("")
        );
    }

    @Test
    void validateKey_ShouldStandardizeFormat() {
        // Given
        String dirtyKey = "Test KEY with Spaces!!";

        // When
        String cleanKey = keyGenerator.validateKey(dirtyKey);

        // Then
        assertEquals("test_key_with_spaces", cleanKey);
    }

    @Test
    void validateKey_ShouldRemoveInvalidCharacters() {
        // Given
        String dirtyKey = "test@key#with$special%chars";

        // When
        String cleanKey = keyGenerator.validateKey(dirtyKey);

        // Then
        assertEquals("testkeywithspecialchars", cleanKey);
    }
}