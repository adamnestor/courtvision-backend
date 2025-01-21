package com.adamnestor.courtvision.web;

import com.adamnestor.courtvision.domain.Users;
import com.adamnestor.courtvision.dto.common.ServiceResponse;
import com.adamnestor.courtvision.dto.picks.*;
import com.adamnestor.courtvision.dto.response.PickResponse;
import com.adamnestor.courtvision.mapper.ResponseMapper;
import com.adamnestor.courtvision.repository.UsersRepository;
import com.adamnestor.courtvision.service.UserPickService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/picks")
public class UserPickController {
    private static final Logger logger = LoggerFactory.getLogger(UserPickController.class);
    private final UserPickService pickService;
    private final UsersRepository usersRepository;
    private final ResponseMapper responseMapper;

    public UserPickController(
            UserPickService pickService,
            UsersRepository usersRepository,
            ResponseMapper responseMapper) {
        this.pickService = pickService;
        this.usersRepository = usersRepository;
        this.responseMapper = responseMapper;
    }

    @GetMapping
    public ResponseEntity<ServiceResponse<List<PickResponse>>> getUserPicks(Authentication auth) {
        try {
            String userEmail = auth.getName();
            Users user = usersRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            
            List<UserPickDTO> picks = pickService.getUserPicks(user);
            List<PickResponse> response = picks.stream()
                    .map(responseMapper::toPickResponse)
                    .collect(Collectors.toList());
                    
            return ResponseEntity.ok(ServiceResponse.success(response));
        } catch (Exception e) {
            logger.error("Error getting user picks", e);
            return ResponseEntity.badRequest()
                    .body(ServiceResponse.error(e.getMessage()));
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