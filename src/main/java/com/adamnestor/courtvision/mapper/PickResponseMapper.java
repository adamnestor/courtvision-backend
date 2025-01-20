package com.adamnestor.courtvision.mapper;

import com.adamnestor.courtvision.dto.picks.UserPickDTO;
import com.adamnestor.courtvision.dto.response.PickResponse;
import com.adamnestor.courtvision.util.ResponseUtils;
import org.springframework.stereotype.Component;

@Component
public class PickResponseMapper {
    public PickResponse toPickResponse(UserPickDTO pick) {
        return new PickResponse(
            pick.id(),
            pick.playerId(),
            pick.playerName(),
            pick.team(),
            pick.opponent(),
            pick.category().toString(),
            pick.threshold(),
            pick.hitRateAtPick(),
            pick.confidenceScore(),
            ResponseUtils.formatGameResult(pick.result()),
            ResponseUtils.formatDateTime(pick.createdAt()),
            pick.game().getGameTime()
        );
    }
} 