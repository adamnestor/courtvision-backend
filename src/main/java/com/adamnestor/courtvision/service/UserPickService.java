package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.dto.picks.*;
import com.adamnestor.courtvision.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserPickService {
    private final UserPicksRepository userPicksRepository;
    private final PlayersRepository playersRepository;
    private final GamesRepository gamesRepository;

    public UserPickService(
            UserPicksRepository userPicksRepository,
            PlayersRepository playersRepository,
            GamesRepository gamesRepository) {
        this.userPicksRepository = userPicksRepository;
        this.playersRepository = playersRepository;
        this.gamesRepository = gamesRepository;
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
        return gamesRepository.findByGameDateAndStatus(LocalDate.now(), GameStatus.SCHEDULED)
                .stream()
                .filter(g -> g.getHomeTeam().equals(player.getTeam()) ||
                        g.getAwayTeam().equals(player.getTeam()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No scheduled game found for player"));
    }

    public List<UserPickDTO> getUserPicks(Users user) {
        List<UserPicks> picks = userPicksRepository.findByUserOrderByCreatedAtDesc(user);
        return picks.stream()
                .filter(pick -> pick.getParlayId() == null)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<ParlayDTO> getUserParlays(Users user) {
        List<String> parlayIds = userPicksRepository.findDistinctParlayIds(user);
        return parlayIds.stream()
                .map(parlayId -> {
                    List<UserPicks> parlayPicks = userPicksRepository.findByUserAndParlayId(user, parlayId);
                    return new ParlayDTO(
                            Long.parseLong(parlayId.split("-")[0]),
                            parlayPicks.stream().map(this::mapToDTO).collect(Collectors.toList()),
                            calculateParlayResult(parlayPicks),
                            parlayPicks.get(0).getCreatedAt()
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
                pick.getCreatedAt()
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

        userPicksRepository.deleteByUserAndParlayId(user, parlayId);
    }
}