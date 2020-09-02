package com.dapeng.seckill.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Service
public class RedisPoolFactory {

    @Autowired
    RedisConfigProperties redisConfigProperties;

    @Bean
    public JedisPool jedisPoolFactory(){
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(redisConfigProperties.getPoolMaxIdle());
        jedisPoolConfig.setMaxTotal(redisConfigProperties.getPoolMaxTotal());
        jedisPoolConfig.setMaxWaitMillis(redisConfigProperties.getPoolMaxWait() * 1000);
        return new JedisPool(jedisPoolConfig,redisConfigProperties.getHost(),redisConfigProperties.getPort(),
                redisConfigProperties.getTimeout()*10000,null,0);
    }
}