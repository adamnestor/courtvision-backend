package com.adamnestor.courtvision.web;

import com.adamnestor.courtvision.domain.Users;
import com.adamnestor.courtvision.domain.UserPicks;
import com.adamnestor.courtvision.dto.common.ServiceResponse;
import com.adamnestor.courtvision.dto.picks.CreatePickRequest;
import com.adamnestor.courtvision.dto.picks.UserPickDTO;
import com.adamnestor.courtvision.repository.UsersRepository;
import com.adamnestor.courtvision.service.UserPickService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/picks")
public class UserPickController {
    private static final Logger logger = LoggerFactory.getLogger(UserPickController.class);

    private final UserPickService pickService;
    private final UsersRepository usersRepository;

    public UserPickController(UserPickService pickService, UsersRepository usersRepository) {
        this.pickService = pickService;
        this.usersRepository = usersRepository;
    }

    @PostMapping
    public ResponseEntity<ServiceResponse<UserPickDTO>> createPick(
            @RequestBody CreatePickRequest request,
            Authentication authentication) {
        try {
            logger.debug("Received pick request: {}", request);

            if (authentication == null) {
                logger.error("No authentication found");
                return ResponseEntity.badRequest()
                        .body(ServiceResponse.error("User not authenticated"));
            }

            String email = authentication.getName();
            Users user = usersRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));

            UserPicks pick = pickService.createPick(user, request);
            return ResponseEntity.ok(ServiceResponse.success(mapToDTO(pick)));
        } catch (Exception e) {
            logger.error("Error creating pick", e);
            return ResponseEntity.badRequest()
                    .body(ServiceResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/parlay")
    public ResponseEntity<ServiceResponse<List<UserPickDTO>>> createParlay(
            @RequestBody List<CreatePickRequest> requests,
            Authentication authentication) {
        try {
            logger.debug("Received parlay request with {} picks", requests.size());

            if (authentication == null) {
                logger.error("No authentication found");
                return ResponseEntity.badRequest()
                        .body(ServiceResponse.error("User not authenticated"));
            }

            String email = authentication.getName();
            Users user = usersRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));

            List<UserPicks> picks = pickService.createParlay(user, requests);
            return ResponseEntity.ok(ServiceResponse.success(
                    picks.stream().map(this::mapToDTO).collect(Collectors.toList())
            ));
        } catch (Exception e) {
            logger.error("Error creating parlay", e);
            return ResponseEntity.badRequest()
                    .body(ServiceResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/today")
    public ResponseEntity<ServiceResponse<List<UserPickDTO>>> getTodaysPicks(
            @AuthenticationPrincipal Users user) {
        try {
            logger.debug("Fetching today's picks for user: {}", user.getEmail());
            List<UserPickDTO> picks = pickService.getTodaysPicks(user)
                    .stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ServiceResponse.success(picks));
        } catch (Exception e) {
            logger.error("Error fetching picks", e);
            return ResponseEntity.badRequest()
                    .body(ServiceResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ServiceResponse<Void>> deletePick(
            @PathVariable Long id,
            @AuthenticationPrincipal Users user) {
        try {
            logger.debug("Deleting pick: {} for user: {}", id, user.getEmail());
            pickService.deletePick(id, user);
            return ResponseEntity.ok(ServiceResponse.success(null));
        } catch (Exception e) {
            logger.error("Error deleting pick", e);
            return ResponseEntity.badRequest()
                    .body(ServiceResponse.error(e.getMessage()));
        }
    }

    private UserPickDTO mapToDTO(UserPicks pick) {
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
}