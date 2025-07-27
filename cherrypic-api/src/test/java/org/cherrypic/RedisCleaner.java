package org.cherrypic;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
public class RedisCleaner {

    private final RedisConnectionFactory connectionFactory;

    public RedisCleaner(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void flushAll() {
        connectionFactory.getConnection().flushDb();
    }
}
