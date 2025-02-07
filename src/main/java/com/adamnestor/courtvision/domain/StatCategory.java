package com.adamnestor.courtvision.domain;

import java.util.Arrays;
import java.util.List;

public enum StatCategory {
    POINTS(20),
    ASSISTS(6),
    REBOUNDS(8);

    private final Integer defaultThreshold;

    StatCategory(Integer defaultThreshold) {
        this.defaultThreshold = defaultThreshold;
    }

    public List<Integer> getValidThresholds() {
        return switch (this) {
            case POINTS -> Arrays.asList(10, 15, 20, 25);
            case ASSISTS -> Arrays.asList(2, 4, 6, 8);
            case REBOUNDS -> Arrays.asList(4, 6, 8, 10);
        };
    }

    public Integer getDefaultThreshold() {
        return defaultThreshold;
    }
}