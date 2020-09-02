package com.dapeng.seckill.access;

import com.dapeng.seckill.bean.SeckillUser;

/**
 * 用于保存用户，ThreadLocal可以为当前线程关联一个数据。
 * 使用ThreadLocal保存用户，因为ThreadLocal是线程安全的，使用ThreadLocal可以保存当前线程持有的对象
 * 每个用户的请求对应一个线程，所以使用ThreadLocal以线程为键保存用户是合适的
 */
public class UserContext {

    private static ThreadLocal<SeckillUser> userHolder = new ThreadLocal<>();

    public static void setUser(SeckillUser user) {
        userHolder.set(user);
    }

    public static SeckillUser getUser() {
        return userHolder.get();
    }
}