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

    @Transactional
    @Override
    public Result seckillVoucher(Long voucherId) {
        //抢购秒杀券
        //1.查询秒杀券
        SeckillVoucher seckillVoucher = seckillVoucherService.query().eq("voucher_id", voucherId).one();
        //2.判断是否可抢，是否在抢购时间内，库存是否充足
        if (seckillVoucher == null
                || !seckillVoucher.getBeginTime().isBefore(LocalDateTime.now())
                || !seckillVoucher.getEndTime().isAfter(LocalDateTime.now())
                || seckillVoucher.getStock() < 1) {
            //3.不满足条件直接返回
            return Result.fail("秒杀券不可抢购！");
        }
        //4.扣减库存，创建订单
        boolean success = seckillVoucherService.update().setSql("stock = stock - 1").
                eq("voucher_id", voucherId).update();
        if (!success){
            return Result.fail("库存不足！");
        }
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("seckill_voucher_order");
        Long userId = UserHolder.getUser().getId();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setStatus(1);
        voucherOrder.setVoucherId(voucherId);
        //5.将订单写入数据库
        this.save(voucherOrder);
        //6.返回订单id
        return Result.ok("下单成功，订单编号：" + orderId);
    }
}
