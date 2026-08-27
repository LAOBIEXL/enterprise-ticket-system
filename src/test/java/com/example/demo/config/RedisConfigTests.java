package com.example.demo.config;

import com.example.demo.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisConfigTests {

    @Test
    void shouldUseStringKeysAndJsonValues() {
        RedisTemplate<String, Object> redisTemplate = new RedisConfig()
                .redisTemplate(mock(RedisConnectionFactory.class));

        assertThat(redisTemplate.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(redisTemplate.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(redisTemplate.getValueSerializer()).isInstanceOf(GenericJacksonJsonRedisSerializer.class);
        assertThat(redisTemplate.getHashValueSerializer()).isInstanceOf(GenericJacksonJsonRedisSerializer.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSerializeAndDeserializeUserAsJson() {
        RedisTemplate<String, Object> redisTemplate = new RedisConfig()
                .redisTemplate(mock(RedisConnectionFactory.class));
        RedisSerializer<Object> serializer =
                (RedisSerializer<Object>) redisTemplate.getValueSerializer();
        User user = new User(1L, "Jack", 20, "jack@example.com",
                LocalDateTime.of(2026, 8, 27, 15, 30),
                LocalDateTime.of(2026, 8, 27, 15, 31));

        byte[] json = serializer.serialize(user);
        Object restored = serializer.deserialize(json);

        assertThat(new String(json)).contains("Jack");
        assertThat(restored).isEqualTo(user);
    }
}
