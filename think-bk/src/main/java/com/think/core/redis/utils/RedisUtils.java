package com.think.core.redis.utils;

import com.think.common.utils.Utils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 * @author 好市民
 */
@Component
public class RedisUtils {

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * 设置过期key
     * @param key 键值
     * @param value 值
     * @param expire 过期时间（秒）
     */
    public boolean set(String key, Object value, long expire) {
        try {
            redisTemplate.opsForValue().set(key, value, expire, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 设置不过期key
     * @param key 键值
     * @param value 值
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 设置过期key
     * @param key 键值
     * @param value 值
     * @param expire 过期时间（秒）
     */
    public Boolean setIfAbsent(String key, Object value, long expire) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, expire, TimeUnit.SECONDS);
    }

    /**
     * 通过key获取value
     * @param key 键值
     * @return
     */
    public Optional<Object> get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(value);
    }

    /**
     * 通过key获取value
     * @param key 键值
     * @param clazz class对象
     * @param <T> 类型
     * @return
     */
    public <T> Optional<T> get(String key, Class<T> clazz) {
        Optional<Object> optional = get(key);
        return optional.map(value -> {
            if (clazz.isInstance(value)) {
                return clazz.cast(value);
            }
            if (value instanceof Number && Number.class.isAssignableFrom(clazz)) {
                Number number = (Number) value;
                if (clazz == Long.class) {
                    return clazz.cast(number.longValue());
                } else if (clazz == Integer.class) {
                    return clazz.cast(number.intValue());
                } else if (clazz == Double.class) {
                    return clazz.cast(number.doubleValue());
                } else if (clazz == Float.class) {
                    return clazz.cast(number.floatValue());
                }
            }
            return clazz.cast(value);
        });
    }

    /**
     * 获取数组
     * @param key 键值
     * @param clazz class对象
     * @param <T> 类型
     * @return
     */
    public <T> List<T> getList(String key, Class<T> clazz) {
        return get(key).map(obj -> Utils.castList(obj, clazz)).orElseGet(ArrayList::new);
    }

    /**
     * 删除
     * @param key 键值-一个或者多个
     */
    public void delete(String ...key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete(Arrays.asList(key));
            }
        }
    }

    /**
     * 删除
     * @param keys 键值数组
     */
    public void delete(Collection<Object> keys) {
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
    /**
     * 模糊匹配删除
     * @param key
     */
    public void fuzzyDelete(String key) {
        key += key.endsWith(":") ? "*" : ":*";
        Set<Object> keys = redisTemplate.keys(key);
        delete(keys);
    }

    /**
     * 设置过期时间
     * @param key 键值
     * @param expire 过期时间（秒）
     * @return
     */
    public boolean expire(String key, long expire) {
        if (expire > 0) {
            redisTemplate.expire(key, expire, TimeUnit.SECONDS);
            return true;
        }
        return false;
    }

    /**
     * 原子自增，首次创建时设置过期时间（日限次计数场景）
     * @param key 键值
     * @param expire 过期时间（秒），仅首次自增时设置
     * @return 自增后的计数值
     */
    public long increment(String key, long expire) {
        Long value = redisTemplate.opsForValue().increment(key);
        if (value != null && value == 1L && expire > 0) {
            redisTemplate.expire(key, expire, TimeUnit.SECONDS);
        }
        return value != null ? value : 0L;
    }

    /**
     * 原子自减（仅用于回滚误自增的日限次计数，如 AI 生图失败后的额度返还）
     * <p>
     * 不重新设 TTL（回滚发生在同一次请求内、key 已由 increment 创建）；
     * 下限保护：若因并发导致回降到 ≤ 0 则删除 key，避免后续回显剩负数
     * </p>
     * @param key 键值
     * @return 自减后的计数值（无则 0）
     */
    public long decrement(String key) {
        Long value = redisTemplate.opsForValue().decrement(key);
        if (value != null && value <= 0L) {
            redisTemplate.delete(key);
            return 0L;
        }
        return value != null ? value : 0L;
    }

    /**
     * 获取过期时间
     * @param key 键值
     * @return 时间（秒）：0-永久有效
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     * @param key 键值
     * @return
     */
    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }
}

