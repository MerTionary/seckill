package com.dapeng.seckill.service;

import com.dapeng.seckill.bean.OrderInfo;
import com.dapeng.seckill.bean.SeckillOrder;
import com.dapeng.seckill.bean.SeckillUser;
import com.dapeng.seckill.dao.OrderDao;
import com.dapeng.seckill.redis.OrderKeyPrefix;
import com.dapeng.seckill.redis.RedisService;
import com.dapeng.seckill.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class OrderService {

    @Autowired
    OrderDao orderDao;

    @Autowired
    RedisService redisService;

    public SeckillOrder getSeckillOrderByUserIDGoodsId(Long userId, Long goodsId) {
        //从Redis里查(createOder里存进去的)
        // SeckillOrder seckillOrder = orderDao.getSeckillOrderByUserIDGoodsId(userId,goodsId);
        SeckillOrder seckillOrder = redisService.get(OrderKeyPrefix.getSeckillOrderByUidGid, "" + userId + goodsId, SeckillOrder.class);
        return seckillOrder;
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public OrderInfo createOrder(SeckillUser user, GoodsVo goodsVo) {

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCreateDate(new Date());
        orderInfo.setDeliveryAddrId(0L);
        orderInfo.setGoodsCount(1);
        orderInfo.setGoodsId(goodsVo.getId());
        orderInfo.setGoodsName(goodsVo.getGoodsName());
        orderInfo.setGoodsPrice(goodsVo.getSeckillPrice());
        orderInfo.setOrderChannel(1);
        orderInfo.setStatus(0);
        orderInfo.setUserId(user.getId());

        // 将订单信息插入order_info表中
        Long orderId = orderDao.insertOrderInfo(orderInfo);

        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setGoodsId(goodsVo.getId());
        // seckillOrder.setOrderId(orderId);
        seckillOrder.setOrderId(orderInfo.getId());
        seckillOrder.setUserId(user.getId());

        // 将秒杀订单插入seckill_order表中
        orderDao.insertSeckillOrder(seckillOrder);
        // 将秒杀订单存入redis缓存中
        redisService.set(OrderKeyPrefix.getSeckillOrderByUidGid, "" + user.getId() + goodsVo.getId(), seckillOrder);

        return orderInfo;
    }

    public OrderInfo getOrderById(long orderId) {
        return  orderDao.getOrderById(orderId);
    }
}