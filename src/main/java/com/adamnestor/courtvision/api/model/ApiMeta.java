package com.adamnestor.courtvision.api.model;

public class ApiMeta {
    private Integer next_cursor;
    private Integer per_page;
    private Integer total_pages;
    private Integer total_count;
    
    // Getters and setters
    public Integer getNext_cursor() {
        return next_cursor;
    }
    
    public void setNext_cursor(Integer next_cursor) {
        this.next_cursor = next_cursor;
    }
    
    public Integer getPer_page() {
        return per_page;
    }
    
    public void setPer_page(Integer per_page) {
        this.per_page = per_page;
    }
    
    public Integer getTotal_pages() {
        return total_pages;
    }
    
    public void setTotal_pages(Integer total_pages) {
        this.total_pages = total_pages;
    }
    
    public Integer getTotal_count() {
        return total_count;
    }
    
    public void setTotal_count(Integer total_count) {
        this.total_count = total_count;
    }
} 