package com.hmdp.utils;

public interface ILock {

    /**
     * 尝试获取锁
     * @Param timeoutSec 锁持有时间，超过这个时间自动释放锁
     * @return true 成功获取锁，false 获取锁失败
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unlock();
}
