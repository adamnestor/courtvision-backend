package com.adamnestor.courtvision.service;

import com.adamnestor.courtvision.domain.*;
import com.adamnestor.courtvision.dto.picks.CreatePickRequest;
import com.adamnestor.courtvision.repository.GamesRepository;
import com.adamnestor.courtvision.repository.PlayersRepository;
import com.adamnestor.courtvision.repository.UserPicksRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserPickService {
    private final UserPicksRepository userPicksRepository;
    private final PlayersRepository playersRepository;
    private final GamesRepository gamesRepository;

    public UserPickService(UserPicksRepository userPicksRepository,
                           PlayersRepository playersRepository,
                           GamesRepository gamesRepository) {
        this.userPicksRepository = userPicksRepository;
        this.playersRepository = playersRepository;
        this.gamesRepository = gamesRepository;
    }

    @Transactional
    public UserPicks createPick(Users user, CreatePickRequest request) {
        Players player = playersRepository.findById(request.playerId())
                .orElseThrow(() -> new EntityNotFoundException("Player not found"));

        Games game = findTodaysGame(request.playerId());  // Changed this line to pass playerId

        UserPicks pick = new UserPicks();
        pick.setUser(user);
        pick.setPlayer(player);
        pick.setGame(game);
        pick.setCategory(request.category());
        pick.setThreshold(request.threshold());
        pick.setHitRateAtPick(BigDecimal.valueOf(request.hitRateAtPick()));
        pick.setCreatedAt(LocalDateTime.now());

        return userPicksRepository.save(pick);
    }

    @Transactional
    public List<UserPicks> createParlay(Users user, List<CreatePickRequest> requests) {
        Games game = findTodaysGame(requests.get(0).playerId());  // Correct - passing the playerId

        List<UserPicks> parlayPicks = new ArrayList<>();
        String parlayId = UUID.randomUUID().toString();

        for (CreatePickRequest request : requests) {
            Players player = playersRepository.findById(request.playerId())
                    .orElseThrow(() -> new EntityNotFoundException("Player not found: " + request.playerId()));

            UserPicks pick = new UserPicks();
            pick.setUser(user);
            pick.setPlayer(player);
            pick.setGame(game);
            pick.setCategory(request.category());
            pick.setThreshold(request.threshold());
            pick.setHitRateAtPick(BigDecimal.valueOf(request.hitRateAtPick()));
            pick.setParlayId(parlayId);
            pick.setCreatedAt(LocalDateTime.now());

            parlayPicks.add(userPicksRepository.save(pick));
        }

        return parlayPicks;
    }

    private Games findTodaysGame(Long playerId) {
        Players player = playersRepository.findById(playerId)
                .orElseThrow(() -> new IllegalStateException("Player not found"));

        return gamesRepository.findByGameDateAndStatus(LocalDate.now(), GameStatus.SCHEDULED)
                .stream()
                .filter(g -> g.getHomeTeam().equals(player.getTeam()) ||
                        g.getAwayTeam().equals(player.getTeam()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No scheduled game found for player"));
    }

    public List<UserPicks> getTodaysPicks(Users user) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return userPicksRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(
                user,
                startOfDay,
                endOfDay
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
}