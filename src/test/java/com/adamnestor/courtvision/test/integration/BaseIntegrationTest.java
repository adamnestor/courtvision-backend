package com.adamnestor.courtvision.test.integration;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.GamesRepository;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.repository.TeamsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Autowired
    protected TeamsRepository teamsRepository;

    @Autowired
    protected GamesRepository gamesRepository;

    protected Teams createTestTeam() {
        Teams team = new Teams();
        team.setName("Denver Nuggets");
        team.setCity("Denver");
        team.setAbbreviation("DEN");
        team.setConference(Conference.West);
        team.setDivision("Northwest");
        team.setExternalId(1L);
        team.setCreatedAt(LocalDateTime.now());
        team.setUpdatedAt(LocalDateTime.now());
        return teamsRepository.save(team);
    }

    protected String createTestPlayer() {
        Teams team = createTestTeam();

        Players player = new Players();
        player.setFirstName("Test");
        player.setLastName("Player");
        player.setStatus(PlayerStatus.ACTIVE);
        player.setTeam(team);
        player.setExternalId(1L);
        player.setPosition("F");
        player.setJerseyNumber("15");
        player.setCreatedAt(LocalDateTime.now());
        player.setUpdatedAt(LocalDateTime.now());
        return playersRepository.save(player).getId().toString();
    }

    protected void createTestGames(String playerId, int numberOfGames) {
        Players player = playersRepository.findById(Long.parseLong(playerId))
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        LocalDateTime gameDate = LocalDateTime.now();
        for (int i = 0; i < numberOfGames; i++) {
            Games game = new Games();
            game.setHomeTeam(player.getTeam());
            game.setAwayTeam(player.getTeam());
            game.setGameDate(gameDate);
            game.setSeason(2024);
            game.setStatus(GameStatus.FINAL);
            game.setExternalId((long) i);
            game.setHomeTeamScore(100);
            game.setAwayTeamScore(95);
            game.setCreatedAt(LocalDateTime.now());
            game.setUpdatedAt(LocalDateTime.now());
            Games savedGame = gamesRepository.save(game);  // Changed from teamsRepository to gamesRepository

            GameStats stats = new GameStats();
            stats.setGame(savedGame);
            stats.setPlayer(player);
            stats.setPoints(20 + i);
            stats.setAssists(5 + (i % 3));
            stats.setRebounds(8 + (i % 4));
            stats.setMinutesPlayed("32:00");
            stats.setCreatedAt(LocalDateTime.now());
            gameStatsRepository.save(stats);

            gameDate = gameDate.minusDays(1);
        }
    }
}