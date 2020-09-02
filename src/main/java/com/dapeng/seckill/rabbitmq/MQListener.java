package com.dapeng.seckill.rabbitmq;

import com.dapeng.seckill.bean.OrderInfo;
import com.dapeng.seckill.bean.SeckillOrder;
import com.dapeng.seckill.bean.SeckillUser;
import com.dapeng.seckill.config.MQConfig;
import com.dapeng.seckill.redis.GoodsKeyPrefix;
import com.dapeng.seckill.redis.RedisService;
import com.dapeng.seckill.result.CodeMsg;
import com.dapeng.seckill.result.Result;
import com.dapeng.seckill.service.GoodsService;
import com.dapeng.seckill.service.OrderService;
import com.dapeng.seckill.service.SeckillService;
import com.dapeng.seckill.service.SeckillUserService;
import com.dapeng.seckill.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MQListener {

    @Autowired
    SeckillUserService userService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    SeckillService seckillService;

    @Autowired
    RedisService redisService;

    @Autowired
    MQSender mqSender;

    private static Logger logger = LoggerFactory.getLogger(MQListener.class);

    @Autowired
    RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = {MQConfig.QUEUE})
    public void receive(String meaasge) {
        // Object o = rabbitTemplate.receiveAndConvert("queue.news");
        logger.info("receive message" + meaasge);
        System.out.println("QUEUE收到了");
    }

    @RabbitListener(queues = {MQConfig.TOPIC_QUEUE_1})
    public void receiveTopic1(String meaasge) {
        // Object o = rabbitTemplate.receiveAndConvert("queue.news");
        logger.info("receive TOPIC_QUEUE_1 message" + meaasge);
        System.out.println("QUEUETOPIC_QUEUE_1收到了");
    }


    @RabbitListener(queues = {MQConfig.TOPIC_QUEUE_2})
    public void receiveTopic2(String meaasge) {
        // Object o = rabbitTemplate.receiveAndConvert("queue.news");
        logger.info("receive TOPIC_QUEUE_2 message" + meaasge);
        System.out.println("TOPIC_QUEUE_2收到了");
    }

    /*
     * 从SECKILL_QUEUE中获取秒杀Message
     */
    @RabbitListener(queues = {MQConfig.SECKILL_QUEUE})
    public void receiveSeckillQueue(SeckillMessage message) {
        logger.info("receiveSeckillQueue" + message);
        SeckillUser user = message.getUser();
        Long goodsId = message.getGoodsId();

        // （这里有个问题，在redis预减库存，入队，但实际队列中的秒杀可能失败，mysql还有库存时，而redis已经没有了，出现错误！）
        // （比如队列里10个请求，两个是同一人的，肯定有一个请求失败，但此时redis中已经减去了这个，mysql并没有减去！）

        //MQ出队请求信息
        //1、从数据库查询并判断库存
        GoodsVo goodsVo = goodsService.getGoodsVoByGoodsId(goodsId);//假如10个商品，某用户同时发出了两个请求req1 req2进来（都没有查到秒杀记录，然后秒杀到了两个产品，sql解决）
        Integer stockCount = goodsVo.getStockCount();
        if (stockCount <= 0) return;

        //2、判断是否当前用户是否已经秒杀到了，防止超卖(createOrder是把订单信息同时插入了mysql和redis，但查的时候只从redis查)
        SeckillOrder seckillOrder = orderService.getSeckillOrderByUserIDGoodsId(user.getId(), goodsId);
        if (seckillOrder != null) {
            // //若redis已经有秒杀到的记录，redis回滚，让其预减的库存加回去(但seckill()失败事务控制不会来到这里，怎么办？)
            // redisService.incr(GoodsKeyPrefix.goodsStockListKeyPrefix, "" + goodsId);
            // System.out.println("redis回滚");
            return;
        }

        //3、减库存-下订单-写入秒杀订单(原子操作，事务)
        OrderInfo orderInfo = seckillService.seckill(user, goodsVo);

    }
}