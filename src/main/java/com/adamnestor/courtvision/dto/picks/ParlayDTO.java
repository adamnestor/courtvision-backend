package com.adamnestor.courtvision.dto.picks;

import java.time.LocalDate;
import java.util.List;

public record ParlayDTO(
        String id,
        List<UserPickDTO> picks,
        Boolean result,
        LocalDate createdAt,
        String createdTime
) {}