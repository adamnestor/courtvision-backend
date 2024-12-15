package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.repository.GameStatsRepository;
import com.adamnestor.courtvision.repository.UserPicksRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PickResultService {
    private static final Logger logger = LoggerFactory.getLogger(PickResultService.class);

    private final UserPicksRepository userPicksRepository;
    private final GameStatsRepository gameStatsRepository;

    public PickResultService(UserPicksRepository userPicksRepository,
                             GameStatsRepository gameStatsRepository) {
        this.userPicksRepository = userPicksRepository;
        this.gameStatsRepository = gameStatsRepository;
    }

    @Scheduled(cron = "0 0 4 * * *") // Run at 4am daily
    public void processPreviousDayResults() {
        logger.info("Starting daily pick result processing");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        processResultsForDate(yesterday);
    }

    public void processResultsForDate(LocalDate date) {
        List<UserPicks> picks = userPicksRepository.findPicksByGameDate(date);
        logger.info("Processing {} picks for date: {}", picks.size(), date);

        for (UserPicks pick : picks) {
            try {
                processPickResult(pick);
            } catch (Exception e) {
                logger.error("Error processing pick {}: {}", pick.getId(), e.getMessage());
            }
        }
    }

    public void processPickResult(UserPicks pick) {
        Optional<GameStats> stats = gameStatsRepository
                .findByPlayerAndGame(pick.getPlayer(), pick.getGame());

        if (stats.isEmpty()) {
            logger.warn("No stats found for player {} in game {}",
                    pick.getPlayer().getId(), pick.getGame().getId());
            return;
        }

        Integer actualValue = switch (pick.getCategory()) {
            case POINTS -> stats.get().getPoints();
            case ASSISTS -> stats.get().getAssists();
            case REBOUNDS -> stats.get().getRebounds();
            default -> throw new IllegalStateException("Invalid category: " + pick.getCategory());
        };

        pick.setResultValue(actualValue);
        pick.setResult(actualValue >= pick.getThreshold());
        userPicksRepository.save(pick);

        if (pick.getParlayId() != null) {
            processParlayResults(pick.getParlayId());
        }
    }

    private void processParlayResults(String parlayId) {
        List<UserPicks> parlayPicks = userPicksRepository.findByParlayId(parlayId);

        // Only process if all picks have results
        if (parlayPicks.stream().allMatch(pick -> pick.getResult() != null)) {
            boolean parlaySuccess = parlayPicks.stream()
                    .allMatch(pick -> Boolean.TRUE.equals(pick.getResult()));

            // Update parlay result for all picks
            parlayPicks.forEach(pick -> {
                pick.setParlayResult(parlaySuccess);
                userPicksRepository.save(pick);
            });
        }
    }
}