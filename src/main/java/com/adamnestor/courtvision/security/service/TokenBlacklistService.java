package com.adamnestor.courtvision.security.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class TokenBlacklistService {
    private final ConcurrentMap<String, Boolean> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklistToken(String token) {
        blacklistedTokens.put(token, true);
    }

    public boolean isBlacklisted(String token) {
        return blacklistedTokens.getOrDefault(token, false);
    }
} 