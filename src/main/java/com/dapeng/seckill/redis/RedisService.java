package com.dapeng.seckill.redis;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
public class RedisService {

    @Autowired
    JedisPool jedisPool;

    /**
     * 获取单个对象
     * */
    public <T> T get(KeyPrefix prefix, String key, Class<T> clazz){
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();//客户端
            //生成真正的key
            String realKey = prefix.getPrefix() + key;
            // System.out.println("getRealKey"+realKey);
            String str = jedis.get(realKey);
            T t  = stringToBean(str,clazz);
            return t;
        } finally {
            returnToPool(jedis);
        }
    }

    /**
     * 设置对象
     * */
    public <T> boolean set(KeyPrefix prefix, String key,T value){
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();//客户端
            String str = beanToString(value);
            if (str==null||str.length()<=0) return false;
            //生成真正的key
            String realKey = prefix.getPrefix() + key;
            // System.out.println("setRealKey"+realKey);
            int seconds = prefix.expireSeconds();
            if (seconds<=0){
                jedis.set(realKey,str);
            }else {
                jedis.setex(realKey,seconds,str);//设置过期时间
            }
            return true;
        } finally {
            returnToPool(jedis);
        }
    }

    /**
     * 判断key是否存在
     * */
    public <T> boolean existKey(KeyPrefix prefix, String key){
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();//客户端
            //生成真正的key
            String realKey = prefix.getPrefix() + key;
            return jedis.exists(realKey);
        } finally {
            returnToPool(jedis);
        }
    }

    /**
     * 删除
     * */
    public <T> boolean delete(KeyPrefix prefix, String key){
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();//客户端
            //生成真正的key
            String realKey = prefix.getPrefix() + key;
           long result =  jedis.del(realKey);
           return result>0;
        } finally {
            returnToPool(jedis);
        }
    }

    /**
     * 增加值(原子操作)
     * */
    public <T> Long incr(KeyPrefix prefix, String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();//客户端
            //生成真正的key
            String realKey = prefix.getPrefix() + key;
            return jedis.incr(realKey);//Redis Incr 命令将 key 中储存的数字值增 1
        } finally {
            returnToPool(jedis);
        }
    }


    /**
     * 减少值(原子操作)
     * */
    public <T> Long decr(KeyPrefix prefix, String key) {
        // Decrement the number stored at key by one. If the key does not exist or contains a value of a
        // wrong type, set the key to the value of "0" before to perform the decrement operation
        // 如果key是String类型，就变成0-1=-1
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();//客户端Jedis
            //生成真正的key
            String realKey = prefix.getPrefix() + key;
            //Redis Incr 命令将 key 中储存的数字值减 1
            return jedis.decr(realKey);
        } finally {
            returnToPool(jedis);
        }
    }

    private <T> String beanToString(T value) {
        if (value == null) return null;
        Class<?> clazz = value.getClass();
        if (clazz == int.class || clazz == Integer.class) {
            return "" + value;
        } else if (clazz == String.class) {
            return (String) value;
        } else if (clazz == long.class || clazz == Long.class) {
            return "" + value;
        } else {
            return JSON.toJSONString(value);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T stringToBean(String str, Class<T> clazz) {
        if (str == null || str.length() <= 0 || clazz == null) return null;
        if (clazz == int.class || clazz == Integer.class) {
            return (T) Integer.valueOf(str);
        } else if (clazz == String.class) {
            return (T) str;
        } else if (clazz == long.class || clazz == Long.class) {
            return (T) Long.valueOf(str);
        } else {
            return JSON.toJavaObject(JSON.parseObject(str), clazz);
        }
    }

    private void returnToPool(Jedis jedis) {
        if (jedis!=null) jedis.close();
    }

}