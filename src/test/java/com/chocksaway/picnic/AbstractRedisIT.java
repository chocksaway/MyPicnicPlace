package com.chocksaway.picnic;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Base for integration tests: full application context against a Testcontainers Redis.
 * All subclasses share one context (and one container); Redis is flushed before each test.
 */
@SpringBootTest(properties = "upload.dir=target/it-uploads")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class AbstractRedisIT {

    @Autowired
    protected StringRedisTemplate redis;

    @BeforeEach
    void flushRedis() {
        redis.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }
}
