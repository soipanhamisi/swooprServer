package org.hamisi.swoopdserver.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;
import redis.embedded.RedisServer;

import java.io.IOException;

@Configuration
public class EmbeddedRedisConfig {

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        // Automatically spins up Redis on default port 6379 when the app launches
        redisServer = new RedisServer(6379);
        redisServer.start();
    }

    @PreDestroy
    public void stopRedis() {
        // Smoothly shuts down Redis from RAM when you stop IntelliJ's runner
        if (redisServer != null) {
            redisServer.stop();
        }
    }
}
