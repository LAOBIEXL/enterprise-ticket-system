package com.example.demo.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisUtilsTests {

    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private HashOperations<String, Object, Object> hashOperations;
    private RedisUtils redisUtils;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        redisUtils = new RedisUtils(redisTemplate);
    }

    @Test
    void shouldSetAndGetTypedValue() {
        when(valueOperations.get("user:1")).thenReturn("Jack");

        redisUtils.set("user:1", "Jack", 30, TimeUnit.MINUTES);
        String value = redisUtils.get("user:1", String.class);

        verify(valueOperations).set("user:1", "Jack", 30, TimeUnit.MINUTES);
        assertThat(value).isEqualTo("Jack");
    }

    @Test
    void shouldDelegateHashOperations() {
        when(hashOperations.get("user:1", "name")).thenReturn("Jack");

        redisUtils.hashPut("user:1", "name", "Jack");
        String value = redisUtils.hashGet("user:1", "name", String.class);

        verify(hashOperations).put("user:1", "name", "Jack");
        assertThat(value).isEqualTo("Jack");
    }

    @Test
    void shouldNormalizeNullableBooleanResults() {
        when(redisTemplate.hasKey("missing")).thenReturn(null);
        when(redisTemplate.delete("missing")).thenReturn(false);

        assertThat(redisUtils.hasKey("missing")).isFalse();
        assertThat(redisUtils.delete("missing")).isFalse();
    }

    @Test
    void shouldRejectBlankKey() {
        assertThatThrownBy(() -> redisUtils.get(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Redis key 不能为空");
    }
}
