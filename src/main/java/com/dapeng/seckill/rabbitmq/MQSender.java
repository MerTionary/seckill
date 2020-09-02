package com.dapeng.seckill.rabbitmq;

import com.dapeng.seckill.config.MQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class MQSender {

    private static Logger logger = LoggerFactory.getLogger(MQListener.class);

    @Autowired
    RabbitTemplate rabbitTemplate;

    // 不是java写的，java模拟的tcp通信，调的api
    public void send(Object obj){
        String msg = obj.toString();
        rabbitTemplate.convertAndSend(MQConfig.QUEUE,msg);
        logger.info("send message" + msg);

        //Message需要自己构造一个，可定义消息体内容和消息头
        // rabbitTemplate.send(exchange,routingKey, message);

        //而object默认当成消息体，只需要传入要发送的对象，自动序列化发送给rabbitmq
        // rabbitTemplate.convertAndSend(exchage,routingKey,objec);
        // Map<String,Object> map = new HashMap<String, Object>();
        // map.put("msg","这是第一个消息");
        // map.put("data", Arrays.asList("halo",123,true));

        // rabbitTemplate.convertAndSend("exchange.direct","atguigu.news",new Book("格1局","蜥蜴人"));
    }

    public void sendDirect(Object obj){
        String msg = obj.toString();
        rabbitTemplate.convertAndSend(MQConfig.DIRECT_EXCHANGE,"direct.key",msg);
        logger.info("send message" + msg);
    }


    public void sendTopic(Object obj){
        String msg = obj.toString();
        rabbitTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE,"topic.key1",msg+1);
        rabbitTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE,"topic.key222",msg+2);
        logger.info("send message" + msg);
    }

    /*
     * 发送秒杀信息到Direct交换机
     */
    public void sendSeckillMesssage(SeckillMessage message) {
        rabbitTemplate.convertAndSend(MQConfig.DIRECT_EXCHANGE,"seckill.key",message);
        logger.info("sendSeckillMesssage" + message);
    }
}