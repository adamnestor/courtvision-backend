package com.adamnestor.courtvision.cache;

import com.adamnestor.courtvision.exception.CacheOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
public class CacheSynchronizationService {
    private static final Logger logger = LoggerFactory.getLogger(CacheSynchronizationService.class);
    private static final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * Executes a cache operation with locking and retry mechanism
     */
    @Retryable(
        value = { CacheOperationException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public <T> T executeCacheOperation(String operationKey, Supplier<T> operation) {
        ReentrantLock lock = locks.computeIfAbsent(operationKey, k -> new ReentrantLock());
        
        try {
            boolean locked = lock.tryLock();
            if (!locked) {
                throw new CacheOperationException("Failed to acquire lock for: " + operationKey);
            }
            
            logger.debug("Acquired lock for cache operation: {}", operationKey);
            return operation.get();
            
        } catch (Exception e) {
            logger.error("Error during cache operation: {}", operationKey, e);
            throw new CacheOperationException("Cache operation failed: " + operationKey, e);
        } finally {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    logger.debug("Released lock for cache operation: {}", operationKey);
                }
            } catch (Exception e) {
                logger.warn("Error releasing lock: {}", operationKey, e);
            }
        }
    }

    /**
     * Coordinates cache refresh cycles to prevent conflicts
     */
    public void coordinateCacheRefresh(String cacheType, Runnable refreshOperation) {
        String lockKey = "refresh:" + cacheType;
        ReentrantLock lock = locks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        
        if (lock.tryLock()) {
            try {
                logger.info("Starting coordinated cache refresh for: {}", cacheType);
                refreshOperation.run();
                logger.info("Completed cache refresh for: {}", cacheType);
            } finally {
                lock.unlock();
            }
        } else {
            logger.warn("Cache refresh already in progress for: {}", cacheType);
        }
    }

    /**
     * Validates if a cache operation can proceed
     */
    public boolean validateCacheOperation(String operationKey) {
        ReentrantLock lock = locks.computeIfAbsent("validate:" + operationKey, k -> new ReentrantLock());
        return lock.tryLock();
    }

    /**
     * Cleans up unused locks periodically
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupLocks() {
        locks.entrySet().removeIf(entry -> !entry.getValue().isLocked());
        logger.debug("Cleaned up unused locks. Current lock count: {}", locks.size());
    }
} 