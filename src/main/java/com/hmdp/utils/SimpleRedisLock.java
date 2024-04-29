package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {

    // 锁的key的前缀
    private static final String KEY_PREFIX = "lock:";

    // 具体业务的名称，将前缀和名称拼接后当key
    private String name;

    //SimpleRedisLock不交给ioc容器管理，因此不能用autowired注入，通过构造函数注入
    private StringRedisTemplate stringRedisTemplate;

    // 拼接uuid在value上，保证线程锁的唯一性，释放时只释放自己的锁
    private static final String ID_PREFIX = UUID.randomUUID().toString(true)+"-";

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {

        this.name = name;

        this.stringRedisTemplate = stringRedisTemplate;

    }

    @Override
    public boolean tryLock(long timeoutSec) {
        // 生成key
        String key = KEY_PREFIX + name;

        // 当前线程的线程id作为value
        String threadId = ID_PREFIX + Thread.currentThread().getId();

        // 设置NX和EX
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, threadId, timeoutSec, TimeUnit.SECONDS);
        // 自动拆箱可能会出现null报错，因此用Boolean.TRUE.equals(success)
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        // 删除锁之前，判断当前锁的value是否是自己存入的
        String value = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);

        // 当前线程的线程id作为value
        String threadId = ID_PREFIX + Thread.currentThread().getId();

        if (threadId.equals(value)){
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }
    }
}
