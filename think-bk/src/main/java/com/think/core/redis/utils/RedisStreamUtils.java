package com.think.core.redis.utils;

import cn.hutool.json.JSONUtil;
import com.think.core.redis.pojo.Message;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Redis Stream 工具类
 * @author 好市民
 */
@Component
public class RedisStreamUtils {

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * 添加
     * @param key KEY
     * @param message 消息
     * @return
     */
    public <T> RecordId add(String key, Message message) {
        ObjectRecord<Object, Object> record = ObjectRecord.create(key, JSONUtil.toJsonStr(message));
        return redisTemplate.opsForStream().add(record);
    }

    /**
     * 确认消费
     * @param key KEY
     * @param group 分组
     * @param recordIds 分组
     * @return
     */
    public Long ack(String key, String group, String... recordIds){
        return redisTemplate.opsForStream().acknowledge(key, group, recordIds);
    }
    /**
     * 确认消费
     * @param group 分组
     * @param record 记录
     * @return
     */
    public Long ack(String group, Record record){
        return redisTemplate.opsForStream().acknowledge(group, record);
    }

    /**
     * 删除
     * @param key KEY
     * @param recordIds 记录ID
     * @return
     */
    public Long delete(String key, String ...recordIds) {
       return redisTemplate.opsForStream().delete(key, recordIds);
    }

}

