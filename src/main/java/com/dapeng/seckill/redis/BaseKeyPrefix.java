package com.dapeng.seckill.redis;

public abstract class BaseKeyPrefix implements KeyPrefix{

    private int expireSecond;

    private String prefix;

    /**
     * 默认过期时间为0，即不过期，过期时间只收到redis的缓存策略影响
     *
     * @param prefix 前缀
     */
    public BaseKeyPrefix(String prefix) {
        this(0,prefix);
    }

    public BaseKeyPrefix(int expireSecond, String prefix) {
        this.expireSecond = expireSecond;
        this.prefix = prefix;
    }


    @Override
    public int expireSeconds() {//过期时间：永不过期
        return expireSecond;
    }

    /**
     * 前缀为模板类的实现类的类名
     * @return
     */
    @Override
    public String getPrefix() {
        String className = getClass().getSimpleName();
        return className + ":" + prefix;
    }
}