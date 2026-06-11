package org.example.cloud.demo.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import redis.embedded.RedisServer;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class EmbeddedRedisConfig {

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() {
        try {
            redisServer = new RedisServer(6379);
            redisServer.start();
            log.info("====== 嵌入内存 Redis 启动成功，监听端口: 6379 ======");
        } catch (Exception e) {
            log.error("嵌入 Redis 启动失败: {}", e.getMessage());
            log.warn("系统将回退使用 JVM 本地缓存");
        }
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            try {
                redisServer.stop();
                log.info("====== 嵌入内存 Redis 已安全停止 ======");
            } catch (Exception e) {
                log.warn("嵌入 Redis 停止时发生异常: {}", e.getMessage());
            }
        }
    }
}
