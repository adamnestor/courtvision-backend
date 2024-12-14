package com.adamnestor.courtvision.web;

import com.adamnestor.courtvision.domain.Users;
import com.adamnestor.courtvision.dto.common.ServiceResponse;
import com.adamnestor.courtvision.dto.picks.*;
import com.adamnestor.courtvision.service.UserPickService;
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

    public UserPickController(UserPickService pickService) {
        this.pickService = pickService;
    }

    @GetMapping
    public ResponseEntity<ServiceResponse<Map<String, Object>>> getUserPicks(Authentication auth) {
        try {
            Users user = (Users) auth.getPrincipal();
            Map<String, Object> response = new HashMap<>();
            response.put("singles", pickService.getUserPicks(user));
            response.put("parlays", pickService.getUserParlays(user));
            return ResponseEntity.ok(ServiceResponse.success(response));
        } catch (Exception e) {
            logger.error("Error fetching picks", e);
            return ResponseEntity.badRequest()
                    .body(ServiceResponse.error("Failed to load picks"));
        }
    }

    @PostMapping
    public ResponseEntity<ServiceResponse<UserPickDTO>> createPick(
            @Valid @RequestBody CreatePickRequest request,
            Authentication auth) {
        try {
            Users user = (Users) auth.getPrincipal();
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
            Users user = (Users) auth.getPrincipal();
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
            Users user = (Users) auth.getPrincipal();
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
            Users user = (Users) auth.getPrincipal();
            pickService.deleteParlay(parlayId, user);
            return ResponseEntity.ok(ServiceResponse.success(null));
        } catch (Exception e) {
            logger.error("Error deleting parlay", e);
            return ResponseEntity.badRequest()
                    .body(ServiceResponse.error(e.getMessage()));
        }
    }
}