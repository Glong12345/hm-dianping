package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private ISeckillVoucherService seckillVoucherService;


    @Override
    public Result seckillVoucher(Long voucherId) {
        //抢购秒杀券
        //1.查询秒杀券
        SeckillVoucher seckillVoucher = seckillVoucherService.query().eq("voucher_id", voucherId).one();
        //2.判断是否可抢，是否在抢购时间内，库存是否充足
        //3.不满足条件直接返回
        if (seckillVoucher == null) {
            return Result.fail("秒杀券不可抢购！");
        }
        if (seckillVoucher.getStock() < 1) {
            return Result.fail("库存不足！");
        }
        if (!seckillVoucher.getBeginTime().isBefore(LocalDateTime.now())
                || !seckillVoucher.getEndTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀券不在有效时间！");
        }

        // 创建订单的逻辑封装，并加上事务控制和用户锁，保证一个用户只能买一单
        Long userId = UserHolder.getUser().getId();
        // 由于toString的源码是new String，因此只用userId.toString()，每次生成的都是新的字符串，因此要加上intern()，如果字符串常量池存在这个一个字符串，则返回池中的字符串，即可以保证同一个id字符串加锁
        synchronized (userId.toString().intern()) {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
    }


    @Override
    public Result createVoucherOrder(Long voucherId) {
        //4.解决一人一单问题，只有在用户未下过单的情况下，才可以抢购
        Long userId = UserHolder.getUser().getId();

        Integer count = this.query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 4.1 已经抢购过则直接返回
        if (count > 0) {
            return Result.fail("用户已经抢购过！");
        }
        // 4.2 没有抢购过则进行抢购

        //5.扣减库存，创建订单
        //5.1 这里通过添加乐观锁，在扣减库存时判断库存是否还大于0来判断是否产生超卖的情况
        boolean success = seckillVoucherService.update().setSql("stock = stock - 1").
                eq("voucher_id", voucherId).gt("stock", 0).update();
        //5.2 判断是否扣减成功
        if (!success) {
            return Result.fail("库存不足！");
        }
        //5.3 构建订单信息
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("seckill_voucher_order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setStatus(1);
        voucherOrder.setVoucherId(voucherId);
        //6.将订单写入数据库
        this.save(voucherOrder);
        //7.返回订单id
        return Result.ok("下单成功，订单编号：" + orderId);


    }
}
