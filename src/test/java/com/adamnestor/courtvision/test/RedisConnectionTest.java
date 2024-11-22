package com.adamnestor.courtvision.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class RedisConnectionTest {

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Test
    public void testRedisConnection() {
        assertThat(connectionFactory.getConnection().ping()).isEqualTo("PONG");
    }
}