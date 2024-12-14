package com.adamnestor.courtvision.web;

import com.adamnestor.courtvision.domain.Users;
import com.adamnestor.courtvision.dto.common.ServiceResponse;
import com.adamnestor.courtvision.dto.picks.*;
import com.adamnestor.courtvision.repository.UsersRepository;
import com.adamnestor.courtvision.service.UserPickService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @GetMapping
    public ResponseEntity<ServiceResponse<Map<String, Object>>> getUserPicks(Authentication auth) {
        try {
            String userEmail = auth.getName();
            logger.debug("1. Starting getUserPicks for: {}", userEmail);

            Users user = usersRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            logger.debug("2. Found user with ID: {}", user.getId());

            List<UserPickDTO> singles = pickService.getUserPicks(user);
            logger.debug("3. Singles fetched: {}", singles);

            List<ParlayDTO> parlays = pickService.getUserParlays(user);
            logger.debug("4. Parlays fetched: {}", parlays);

            Map<String, Object> response = new HashMap<>();
            response.put("singles", singles);
            response.put("parlays", parlays);
            logger.debug("5. Final response map: {}", response);

            return ResponseEntity.ok(ServiceResponse.success(response));
        } catch (Exception e) {
            logger.error("Error in getUserPicks", e);
            return ResponseEntity.badRequest()
                    .body(ServiceResponse.error("Failed to load picks: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ServiceResponse<UserPickDTO>> createPick(
            @Valid @RequestBody CreatePickRequest request,
            Authentication auth) {
        try {
            String userEmail = auth.getName();
            Users user = usersRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            return ResponseEntity.ok(ServiceResponse.success(
                    pickService.mapToDTO(pickService.createPick(user, request))
            ));
        } catch (Exception e) {
            logger.error("Error creating pick", e);
            return ResponseEntity.badRequest()
                    .body(ServiceResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/parlay")
    public ResponseEntity<ServiceResponse<List<UserPickDTO>>> createParlay(
            @Valid @RequestBody List<CreatePickRequest> requests,
            Authentication auth) {
        try {
            String userEmail = auth.getName();
            Users user = usersRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            return ResponseEntity.ok(ServiceResponse.success(
                    pickService.createParlay(user, requests).stream()
                            .map(pickService::mapToDTO)
                            .toList()
            ));
        } catch (Exception e) {
            logger.error("Error creating parlay", e);
            return ResponseEntity.badRequest()
                    .body(ServiceResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ServiceResponse<Void>> deletePick(
            @PathVariable Long id,
            Authentication auth) {
        try {
            String userEmail = auth.getName();
            Users user = usersRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            pickService.deletePick(id, user);
            return ResponseEntity.ok(ServiceResponse.success(null));
        } catch (Exception e) {
            logger.error("Error deleting pick", e);
            return ResponseEntity.badRequest()
                    .body(ServiceResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/parlay/{parlayId}")
    public ResponseEntity<ServiceResponse<Void>> deleteParlay(
            @PathVariable String parlayId,
            Authentication auth) {
        try {
            String userEmail = auth.getName();
            Users user = usersRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            pickService.deleteParlay(parlayId, user);
            return ResponseEntity.ok(ServiceResponse.success(null));
        } catch (Exception e) {
            logger.error("Error deleting parlay", e);
            return ResponseEntity.badRequest()
                    .body(ServiceResponse.error(e.getMessage()));
        }
    }
}