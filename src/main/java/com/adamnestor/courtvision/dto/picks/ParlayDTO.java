package com.adamnestor.courtvision.dto.picks;

import java.time.LocalDateTime;
import java.util.List;

public record ParlayDTO(
        Long id,
        List<UserPickDTO> picks,
        Boolean result,
        LocalDateTime createdAt
) {}