package com.adamnestor.courtvision.api.model;

public class ApiResponse<T> {
    private T data;
    private ApiMeta meta;
    
    // Constructor
    public ApiResponse(T data, ApiMeta meta) {
        this.data = data;
        this.meta = meta;
    }
    
    // Getters and setters
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public ApiMeta getMeta() {
        return meta;
    }
    
    public void setMeta(ApiMeta meta) {
        this.meta = meta;
    }
} 