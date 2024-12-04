package com.adamnestor.courtvision.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum StatCategory {
    ALL,
    POINTS,
    ASSISTS,
    REBOUNDS;

    public List<Integer> getValidThresholds() {
        return switch (this) {
            case POINTS -> Arrays.asList(10, 15, 20, 25);
            case ASSISTS -> Arrays.asList(2, 4, 6, 8);
            case REBOUNDS -> Arrays.asList(4, 6, 8, 10);
            case ALL -> Collections.emptyList();
        };
    }

    public Integer getDefaultThreshold() {
        return switch (this) {
            case POINTS -> 20;
            case ASSISTS -> 4;
            case REBOUNDS -> 8;
            case ALL -> null;
        };
    }
}