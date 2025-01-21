package com.adamnestor.courtvision.api.model;

public class ApiMeta {
    private Integer totalPages;
    private Integer currentPage;
    private Integer nextPage;
    private Integer perPage;
    private Integer totalCount;

    // Constructor
    public ApiMeta() {}

    // Getters and Setters
    public Integer getTotalPages() { return totalPages; }
    public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }

    public Integer getCurrentPage() { return currentPage; }
    public void setCurrentPage(Integer currentPage) { this.currentPage = currentPage; }

    public Integer getNextPage() { return nextPage; }
    public void setNextPage(Integer nextPage) { this.nextPage = nextPage; }

    public Integer getPerPage() { return perPage; }
    public void setPerPage(Integer perPage) { this.perPage = perPage; }

    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
} 