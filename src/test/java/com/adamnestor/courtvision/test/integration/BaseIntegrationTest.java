package com.adamnestor.courtvision.test.integration;

import com.adamnestor.courtvision.domain.GameStats;
import com.adamnestor.courtvision.domain.PlayerStatus;
import com.adamnestor.courtvision.domain.Players;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected PlayersRepository playersRepository;

    @Autowired
    protected GameStatsRepository gameStatsRepository;

    protected String createTestPlayer() {
        Players player = new Players();
        player.setFirstName("Test");
        player.setLastName("Player");
        player.setStatus(PlayerStatus.ACTIVE);
        return playersRepository.save(player).getId().toString();
    }

    protected void createTestGames(String playerId, int numberOfGames) {
        Players player = playersRepository.findById(Long.parseLong(playerId))
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        LocalDate gameDate = LocalDate.now();
        for (int i = 0; i < numberOfGames; i++) {
            GameStats stats = new GameStats();
            stats.setPlayer(player);
            stats.setPoints(20 + i);
            stats.setAssists(5 + (i % 3));
            stats.setRebounds(8 + (i % 4));
            stats.setMinutesPlayed("32:00");
            gameStatsRepository.save(stats);
            gameDate = gameDate.minusDays(1);
        }
    }
}
