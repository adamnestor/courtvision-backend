package com.adamnestor.courtvision.dto;

public record PageInfo(
    int currentPage,
    int pageSize,
    int totalPages,
    long totalElements
) {}