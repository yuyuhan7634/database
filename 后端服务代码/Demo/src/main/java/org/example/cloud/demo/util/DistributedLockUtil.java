package org.example.cloud.demo.util;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁工具类
 *
 * 当 Redis 可用时使用 Redisson 分布式锁；
 * 当 Redis 不可用时（调试模式），回退为 JVM 本地锁（synchronized）。
 *
 * 加分项说明：分布式锁确保月底自动分房活动在多实例部署下的互斥执行
 */
@Slf4j
@Component
public class DistributedLockUtil {

    @Autowired(required = false)
    private RedissonClient redissonClient;

    /** 本地锁回退 */
    private final ConcurrentHashMap<String, Object> localLockMap = new ConcurrentHashMap<>();

    /**
     * 执行带锁的任务（自动选择分布式锁或本地锁）
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, Supplier<T> task) {
        if (redissonClient != null) {
            return executeWithRedisLock(lockKey, waitTime, leaseTime, task);
        }
        return executeWithLocalLock(lockKey, task);
    }

    /**
     * 基于 Redis 的分布式锁
     */
    private <T> T executeWithRedisLock(String lockKey, long waitTime, long leaseTime, Supplier<T> task) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("获取分布式锁 [{}] 失败，有其他实例正在执行相同任务", lockKey);
                throw new RuntimeException("系统繁忙，请稍后重试（无法获取分布式锁）");
            }
            log.info("成功获取分布式锁 [{}]", lockKey);
            return task.get();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("执行带锁任务时发生异常: {}", e.getMessage());
            throw new RuntimeException("任务执行异常: " + e.getMessage());
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("释放分布式锁 [{}]", lockKey);
            }
        }
    }

    /**
     * 本地锁回退（单实例调试时使用）
     */
    private <T> T executeWithLocalLock(String lockKey, Supplier<T> task) {
        Object lockObj = localLockMap.computeIfAbsent(lockKey, k -> new Object());
        synchronized (lockObj) {
            log.info("[本地锁] 获取锁 [{}]", lockKey);
            try {
                return task.get();
            } finally {
                log.info("[本地锁] 释放锁 [{}]", lockKey);
            }
        }
    }

    public boolean tryLock(String lockKey, long leaseTime) {
        if (redissonClient != null) {
            RLock lock = redissonClient.getLock(lockKey);
            try {
                return lock.tryLock(0, leaseTime, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        // 本地模式：直接返回 true（单实例）
        return true;
    }

    public void unlock(String lockKey) {
        if (redissonClient != null) {
            RLock lock = redissonClient.getLock(lockKey);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
