package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryStoreById(Long id) {
        // 解决缓存穿透
//        Shop shop = queryStoreByIdCachePenetration(id);
        // 解决缓存击穿
        Shop shop = queryStoreByIdHotspotInvalid(id);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    // 缓存击穿问题
    public Shop queryStoreByIdHotspotInvalid(Long id) {
        while (true){
            //1.查询店铺时，先从缓存中查询
            String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
            //2.1 命中缓存，直接返回结果
            if (StrUtil.isNotBlank(shopJson)) {
                Shop shop = JSONUtil.toBean(shopJson, Shop.class);
                return shop;
            }
            //2.2 为了防止缓存穿透命中空字符串
            if (shopJson != null) {
                return null;
            }

            Shop shop = null;
            //3. 未命中，通过使用互斥锁来解决缓存击穿
            String lockKey = LOCK_SHOP_KEY + id;
            //3.1 创建互斥锁
            try {
                boolean lock = tryLock(lockKey);
                //3.2 创建失败，锁存在
                if (!lock) {
                    Thread.sleep(50);
                    continue;
                }
                //3.3 创建成功,开始查询数据库
                shop = getById(id);
                Thread.sleep(200);
                //3.4 数据库查询到店铺信息为空，存入空字符串缓存
                if (shop == null) {
                    stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                    return null;
                }
                //3.5 不为空则存入缓存，并返回结果
                String jsonShopStr = JSONUtil.toJsonStr(shop);
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, jsonShopStr, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
            }catch (InterruptedException e){
                throw new RuntimeException(e);
            }finally {
                // 释放互斥锁
                deleteLock(lockKey);
            }
            return shop;
        }
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


    // 解决缓存穿透的问题
    public Shop queryStoreByIdCachePenetration(Long id) {
        //1.查询店铺时，先从缓存中查询
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //2.1 命中缓存，直接返回结果
        if (StrUtil.isNotBlank(shopJson)) {
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        //2.2 为了防止缓存穿透命中空字符串
        if (shopJson != null) {
            return null;
        }

        //3. 未命中，查询数据库
        Shop shop = getById(id);

        //4. 数据库查询到店铺信息为空，存入空字符串缓存
        if (Objects.isNull(shop)) {
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //5. 不为空则存入缓存，并返回结果
        String jsonShopStr = JSONUtil.toJsonStr(shop);
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, jsonShopStr, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return shop;
    }

    @Transactional
    @Override
    public Result updateShopById(Shop shop) {
        // 为了保存缓存跟数据库的数据一致性，我们先操作数据库，再删除缓存即可
        //1.先判断输入是否合理
        if (Objects.isNull(shop)) {
            return Result.fail("店铺不存在");
        }

        //2. 更新数据库
        updateById(shop);

        //3. 删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());


        return Result.ok();
    }
}
