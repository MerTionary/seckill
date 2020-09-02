package com.dapeng.seckill.redis;

public class SeckillUserKeyPrefix extends BaseKeyPrefix{

    public static final int TOKEN_EXPIRE = 3600 * 24 * 2; // 缓存有效时间为两天

    public SeckillUserKeyPrefix(int expireSecond, String prefix) {
        super(expireSecond,prefix);
    }

    // 用于存储用户对象到redis的key前缀
    public static SeckillUserKeyPrefix token = new SeckillUserKeyPrefix(TOKEN_EXPIRE,"token");

    //对象缓存
    public static SeckillUserKeyPrefix seckillUserKeyPrefixById = new SeckillUserKeyPrefix(0,"id");

}