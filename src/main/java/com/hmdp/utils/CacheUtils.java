package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class CacheUtils {

    private final StringRedisTemplate stringRedisTemplate;

    public CacheUtils(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 将任意对象序列化为Json字符串并存入到redis当中做缓存,并设置过期时间
     */
    private void set(String cacheKey, Object obj, long expireTime, TimeUnit timeUnit) {
        stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(obj), expireTime, timeUnit);
    }

    /**
     * 将任意对象序列化为Json字符串并存入到redis当中做缓存,并设置逻辑过期时间
     */
    private void setWithLogicExpire(String cacheKey, Object obj, long expireTime, TimeUnit timeUnit) {
        RedisData redisData = new RedisData();
        redisData.setData(obj);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(expireTime)));
        // 不需要设置过期时间，是逻辑过期
        stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(redisData));
    }


    /**
     * 从redis中获取相应的缓存数据，如果不存在则返回null，以避免缓存穿透
     */
    private <R, ID> R getDataWithCacheThrough(String cacheKey, ID id, Class<R> type, Function<ID, R> queryFun, long expireTime, TimeUnit timeUnit) {
        //1.查询时，先从缓存中查询
        String dataJson = stringRedisTemplate.opsForValue().get(cacheKey);

        //2.1 命中缓存，直接返回结果
        if (StrUtil.isNotBlank(dataJson)) {
            R res = JSONUtil.toBean(dataJson, type);
            return res;
        }
        //2.2 为了防止缓存穿透命中空字符串
        if (dataJson != null) {
            return null;
        }

        //3. 未命中，查询数据库
        R res = queryFun.apply(id);

        //4. 数据库查询到店铺信息为空，存入空字符串缓存
        if (Objects.isNull(res)) {
            this.set(cacheKey, "", expireTime, timeUnit);
            return null;
        }
        //5. 不为空则存入缓存，并返回结果
        this.set(cacheKey, res, expireTime, timeUnit);

        return res;
    }

    //声明一个线程池，完成缓存逻辑过期之后的重构
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 获取热点key的缓存：从redis中获取Json字符串缓存数据并转换成目标对象，如果不存在则返回null，并判断其是否逻辑过期，以避免缓存击穿
     */
    private <R, ID> R getDataWithLogicExpire(String cacheKey, ID id, Class<R> type, Function<ID, R> queryFun, long expireTime, TimeUnit timeUnit) {
        //1. 查询时，先从redis中获取缓存数据
        String dataJson = stringRedisTemplate.opsForValue().get(cacheKey);

        //2 未命中缓存，直接返回null
        if (StrUtil.isBlank(dataJson)) {
            return null;
        }

        //3 命中缓存，先判断是否逻辑过期
        RedisData redisData = JSONUtil.toBean(dataJson, RedisData.class);
        R res = JSONUtil.toBean((JSONObject) redisData.getData(), type);

        //3.1 逻辑过期，开启线程去更新数据
        if (redisData.getExpireTime().isAfter(LocalDateTime.now())) {
            String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
            //3.2 尝试获取互斥锁
            if (tryLock(lockKey)) {
                //todo 获取到锁，要再检查一下缓存数据是否更新

                //3.3 获取到锁，开启线程去更新数据
                CACHE_REBUILD_EXECUTOR.submit(() -> {
                    try {
                        //3.4 查询数据库
                        R r = queryFun.apply(id);
                        //3.5 更新缓存
                        this.setWithLogicExpire(cacheKey, r, expireTime, timeUnit);

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        //3.6 释放锁
                        deleteLock(lockKey);
                    }
                });
            }
        }
        //4 不管过没过期，都返回旧数据
        return res;
    }

    // 创建互斥锁,这里用Redis里的setnx方法来实现，该方法保证只有一个key
    public boolean tryLock(String key) {
        //  获取锁，设置过期时间
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    // 删除互斥锁
    public void deleteLock(String key) {
        stringRedisTemplate.delete(key);
    }
}
