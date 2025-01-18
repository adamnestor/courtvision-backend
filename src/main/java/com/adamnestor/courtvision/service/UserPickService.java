package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.dto.picks.*;
import com.adamnestor.courtvision.repository.*;
import com.adamnestor.courtvision.service.util.DateUtils;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class UserPickService {
    private static final Logger logger = LoggerFactory.getLogger(UserPickService.class);
    private final UserPicksRepository userPicksRepository;
    private final PlayersRepository playersRepository;
    private final GamesRepository gamesRepository;
    private final DateUtils dateUtils;

    public UserPickService(
            UserPicksRepository userPicksRepository,
            PlayersRepository playersRepository,
            GamesRepository gamesRepository,
            DateUtils dateUtils) {
        this.userPicksRepository = userPicksRepository;
        this.playersRepository = playersRepository;
        this.gamesRepository = gamesRepository;
        this.dateUtils = dateUtils;
    }

    public UserPicks createPick(Users user, CreatePickRequest request) {
        Players player = playersRepository.findById(request.playerId())
                .orElseThrow(() -> new EntityNotFoundException("Player not found"));

        Games game = findTodaysGame(player);

        UserPicks pick = new UserPicks();
        pick.setUser(user);
        pick.setPlayer(player);
        pick.setGame(game);
        pick.setCategory(request.category());
        pick.setThreshold(request.threshold());
        pick.setHitRateAtPick(BigDecimal.valueOf(request.hitRateAtPick()));

        return userPicksRepository.save(pick);
    }

    public List<UserPicks> createParlay(Users user, List<CreatePickRequest> requests) {
        String parlayId = UUID.randomUUID().toString();
        List<UserPicks> parlayPicks = new ArrayList<>();

        for (CreatePickRequest request : requests) {
            Players player = playersRepository.findById(request.playerId())
                    .orElseThrow(() -> new EntityNotFoundException("Player not found"));

            Games game = findTodaysGame(player);

            UserPicks pick = new UserPicks();
            pick.setUser(user);
            pick.setPlayer(player);
            pick.setGame(game);
            pick.setCategory(request.category());
            pick.setThreshold(request.threshold());
            pick.setHitRateAtPick(BigDecimal.valueOf(request.hitRateAtPick()));
            pick.setParlayId(parlayId);

            parlayPicks.add(userPicksRepository.save(pick));
        }

        return parlayPicks;
    }

    private Games findTodaysGame(Players player) {
        return gamesRepository.findByGameDateAndStatus(dateUtils.getCurrentEasternDate(), GameStatus.SCHEDULED)
                .stream()
                .filter(g -> g.getHomeTeam().equals(player.getTeam()) ||
                        g.getAwayTeam().equals(player.getTeam()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No scheduled game found for player"));
    }

    public List<UserPickDTO> getUserPicks(Users user) {
        logger.info("Getting picks for user ID: {}", user.getId());
        List<UserPicks> picks = userPicksRepository.findByUserOrderByCreatedAtDesc(user);
        logger.info("Found {} picks in repository", picks.size());
        return picks.stream()
                .filter(pick -> pick.getParlayId() == null)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<ParlayDTO> getUserParlays(Users user) {
        logger.info("Getting parlays for user ID: {}", user.getId());
        List<String> parlayIds = userPicksRepository.findDistinctParlayIds(user);
        logger.info("Found {} distinct parlay IDs", parlayIds.size());

        return parlayIds.stream()
                .map(parlayId -> {
                    List<UserPicks> parlayPicks = userPicksRepository.findByUserAndParlayId(user, parlayId);
                    logger.info("Found {} picks for parlay ID: {}", parlayPicks.size(), parlayId);
                    return new ParlayDTO(
                            parlayId,
                            parlayPicks.stream().map(this::mapToDTO).collect(Collectors.toList()),
                            calculateParlayResult(parlayPicks),
                            parlayPicks.get(0).getCreatedAt(),
                            parlayPicks.get(0).getCreatedTime()
                    );
                })
                .collect(Collectors.toList());
    }

    private Boolean calculateParlayResult(List<UserPicks> parlayPicks) {
        if (parlayPicks.stream().anyMatch(pick -> pick.getResult() == null)) {
            return null;
        }
        return parlayPicks.stream().allMatch(pick -> Boolean.TRUE.equals(pick.getResult()));
    }

    public UserPickDTO mapToDTO(UserPicks pick) {
        return new UserPickDTO(
                pick.getId(),
                pick.getPlayer().getId(),
                pick.getPlayer().getFirstName() + " " + pick.getPlayer().getLastName(),
                pick.getPlayer().getTeam().getAbbreviation(),
                pick.getGame().getHomeTeam().equals(pick.getPlayer().getTeam()) ?
                        pick.getGame().getAwayTeam().getAbbreviation() :
                        pick.getGame().getHomeTeam().getAbbreviation(),
                pick.getCategory(),
                pick.getThreshold(),
                pick.getHitRateAtPick().doubleValue(),
                pick.getResult(),
                pick.getCreatedAt(),
                pick.getCreatedTime()
        );
    }

    public void deletePick(Long id, Users user) {
        UserPicks pick = userPicksRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pick not found"));

        if (!pick.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("User not authorized to delete this pick");
        }

        if (pick.getResult() != null) {
            throw new IllegalStateException("Cannot delete picks that have already been resulted");
        }

        userPicksRepository.delete(pick);
    }

    public void deleteParlay(String parlayId, Users user) {
        List<UserPicks> parlayPicks = userPicksRepository.findByUserAndParlayId(user, parlayId);

        if (parlayPicks.isEmpty()) {
            throw new EntityNotFoundException("Parlay not found");
        }

        if (parlayPicks.stream().anyMatch(pick -> pick.getResult() != null)) {
            throw new IllegalStateException("Cannot delete parlays that have already been resulted");
        }

        logger.debug("Deleting parlay with ID: {} for user: {}", parlayId, user.getEmail());
        userPicksRepository.deleteByUserAndParlayId(user, parlayId);
        logger.debug("Parlay deletion completed for ID: {} and user: {}", parlayId, user.getEmail());
    }
}