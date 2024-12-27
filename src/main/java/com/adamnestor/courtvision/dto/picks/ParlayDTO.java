package com.adamnestor.courtvision.dto.picks;

import java.time.LocalDateTime;
import java.util.List;

public record ParlayDTO(
        String id,
        List<UserPickDTO> picks,
        Boolean result,
        LocalDateTime createdAt
) {}