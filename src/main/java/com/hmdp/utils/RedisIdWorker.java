package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    public static final long BEGIN_TIMESTAMP = 1609459200000L; //设置开始时间 2021-01-01 00:00:00

    private static final int COUNT_BITS = 32;// 序列号长度

    public long nextId(String keyPrefix) {
        //1. 获取当前时间戳
        LocalDateTime now = LocalDateTime.now();
        long currentSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = currentSecond - BEGIN_TIMESTAMP;

        //2. 生成序列号 借助redis中的自增属性，并且按二进制进行拼接
        String date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = stringRedisTemplate.opsForValue().increment("inc:" + keyPrefix + ":" + date);

        //3. 拼接并返回
        return timeStamp << COUNT_BITS | count;
    }
}
