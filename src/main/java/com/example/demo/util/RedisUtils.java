package com.example.demo.util;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 对 {@link RedisTemplate} 常用操作的二次封装。
 */
@Component
public class RedisUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisUtils(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 保存对象，不设置过期时间。 */
    public void set(String key, Object value) {
        checkKey(key);
        Assert.notNull(value, "Redis value 不能为空");
        valueOperations().set(key, value);
    }

    /** 保存对象并设置过期时间。 */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        checkKey(key);
        Assert.notNull(value, "Redis value 不能为空");
        Assert.isTrue(timeout > 0, "Redis 过期时间必须大于 0");
        Assert.notNull(unit, "时间单位不能为空");
        valueOperations().set(key, value, timeout, unit);
    }

    /** 获取对象。不存在时返回 null。 */
    public Object get(String key) {
        checkKey(key);
        return valueOperations().get(key);
    }

    /** 获取对象并校验其类型。不存在时返回 null。 */
    public <T> T get(String key, Class<T> type) {
        Assert.notNull(type, "目标类型不能为空");
        Object value = get(key);
        return value == null ? null : type.cast(value);
    }

    /** 删除一个 key。 */
    public boolean delete(String key) {
        checkKey(key);
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /** 判断 key 是否存在。 */
    public boolean hasKey(String key) {
        checkKey(key);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /** 为已有 key 设置过期时间。 */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        checkKey(key);
        Assert.isTrue(timeout > 0, "Redis 过期时间必须大于 0");
        Assert.notNull(unit, "时间单位不能为空");
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    /** 获取剩余过期时间；-1 表示永久，-2 表示 key 不存在。 */
    public long getExpire(String key, TimeUnit unit) {
        checkKey(key);
        Assert.notNull(unit, "时间单位不能为空");
        Long timeout = redisTemplate.getExpire(key, unit);
        return timeout == null ? -2L : timeout;
    }

    /** 对数字值执行原子增减，delta 可为负数。 */
    public long increment(String key, long delta) {
        checkKey(key);
        Long result = valueOperations().increment(key, delta);
        Assert.state(result != null, "Redis 自增操作未返回结果");
        return result;
    }

    /** 向 Hash 中写入一个字段。 */
    public void hashPut(String key, String hashKey, Object value) {
        checkKey(key);
        checkHashKey(hashKey);
        Assert.notNull(value, "Redis Hash value 不能为空");
        hashOperations().put(key, hashKey, value);
    }

    /** 从 Hash 中读取字段。不存在时返回 null。 */
    public Object hashGet(String key, String hashKey) {
        checkKey(key);
        checkHashKey(hashKey);
        return hashOperations().get(key, hashKey);
    }

    /** 从 Hash 中读取字段并校验其类型。 */
    public <T> T hashGet(String key, String hashKey, Class<T> type) {
        Assert.notNull(type, "目标类型不能为空");
        Object value = hashGet(key, hashKey);
        return value == null ? null : type.cast(value);
    }

    /** 获取整个 Hash。 */
    public Map<Object, Object> hashEntries(String key) {
        checkKey(key);
        return hashOperations().entries(key);
    }

    /** 删除 Hash 中的一个或多个字段，返回成功删除的数量。 */
    public long hashDelete(String key, Object... hashKeys) {
        checkKey(key);
        Assert.notEmpty(hashKeys, "至少需要提供一个 Hash key");
        return hashOperations().delete(key, hashKeys);
    }

    private ValueOperations<String, Object> valueOperations() {
        return redisTemplate.opsForValue();
    }

    private HashOperations<String, Object, Object> hashOperations() {
        return redisTemplate.opsForHash();
    }

    private void checkKey(String key) {
        Assert.hasText(key, "Redis key 不能为空");
    }

    private void checkHashKey(String hashKey) {
        Assert.hasText(hashKey, "Redis Hash key 不能为空");
    }
}
