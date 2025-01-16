package com.adamnestor.courtvision.test.cache;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.service.cache.CacheKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.interceptor.SimpleKey;
import java.lang.reflect.Method;
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
    void todaysGamesKey_ShouldGenerateCorrectFormat() {
        String key = keyGenerator.todaysGamesKey();
        String expectedDate = LocalDate.now().toString();

        assertTrue(key.startsWith("games:"));
        assertTrue(key.endsWith(expectedDate));
    }

    @Test
    void hitRatesKey_ShouldGenerateCorrectFormat() {
        String key = keyGenerator.hitRatesKey(
                testPlayer,
                StatCategory.POINTS,
                20,
                TimePeriod.L10
        );

        assertEquals("hitrate:1:points:20:l10", key.toLowerCase());
    }

    @Test
    void playerStatsKey_ShouldGenerateCorrectFormat() {
        String key = keyGenerator.playerStatsKey(testPlayer, TimePeriod.L10);
        assertEquals("playerstats:1:l10", key.toLowerCase());
    }

    @Test
    void generate_ShouldHandleNullParameters() throws Exception {
        // Get a test method from this test class
        Method method = this.getClass().getDeclaredMethod("setUp");

        Object result = keyGenerator.generate(this, method, (Object[]) null);

        assertNotNull(result);
        assertTrue(result.toString().contains(this.getClass().getSimpleName()));
        assertTrue(result.toString().contains("setup"));
    }

    @Test
    void generate_ShouldHandlePlayerParameter() throws Exception {
        Method method = this.getClass().getDeclaredMethod("setUp");

        Object result = keyGenerator.generate(this, method, testPlayer);

        assertNotNull(result);
        assertTrue(result.toString().contains("player1"));
    }

    @Test
    void generate_ShouldHandleMultipleParameters() throws Exception {
        Method method = this.getClass().getDeclaredMethod("setUp");

        Object result = keyGenerator.generate(
                this,
                method,
                testPlayer,
                StatCategory.POINTS,
                TimePeriod.L10
        );

        String key = result.toString().toLowerCase();
        assertTrue(key.contains("player1"));
        assertTrue(key.contains("points"));
        assertTrue(key.contains("l10"));
    }

    @Test
    void generate_ShouldHandleMixedParameters() throws Exception {
        Method method = this.getClass().getDeclaredMethod("setUp");

        Object result = keyGenerator.generate(
                this,
                method,
                testPlayer,
                "testString",
                42,
                true
        );

        String key = result.toString().toLowerCase();
        assertTrue(key.contains("player1"));
        assertTrue(key.contains("teststring"));
        assertTrue(key.contains("42"));
        assertTrue(key.contains("true"));
    }
}