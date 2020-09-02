package com.dapeng.seckill.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


/**
 * 自动配置(引入spring-boot-starter-amqp)
 *  1、RabbitAutoConfiguration
 *  2、有自动配置了连接工厂ConnectionFactory；
 *  3、RabbitProperties 封装了 RabbitMQ的配置
 *  4、 RabbitTemplate ：给RabbitMQ发送和接受消息；
 *  5、 AmqpAdmin： RabbitMQ系统管理功能组件;
 *  	AmqpAdmin：创建和删除Queue、Exchange、Binding
 *  6、@EnableRabbit  +  @RabbitListener 监听消息队列的内容
 *
 */
@Configuration
public class MQConfig {

    @Autowired
    AmqpAdmin amqpAdmin;

    public static final String SECKILL_QUEUE="seckill.queue";
    public static final String QUEUE="queue";
    public static final String TOPIC_QUEUE_1="topic.queue1";
    public static final String TOPIC_QUEUE_2="topic.queue2";

    public static final String DIRECT_EXCHANGE="direct.exchange";
    public static final String TOPIC_EXCHANGE="topic.exchange";
    public static final String ROUTING_KEY_1="topic.key1";
    public static final String ROUTING_KEY_2="topic.#";
    public static final String HEADER_QUEUE = "header.queue";
    public static final String FANOUT_EXCHANGE = "fanoutExchange";
    public static final String HEADERS_EXCHANGE = "headersExchange";

    /*
     * Direct模式 交换机exchange
     */
    @Bean
    public Queue queue(){
        return new Queue(QUEUE,true);
    }
    @Bean
    public Queue seckillQueue(){
        return new Queue(SECKILL_QUEUE,true);
    }

    /**
     * Topic模式Queue
     */
    @Bean
    public Queue topicQueue1(){
        return new Queue(TOPIC_QUEUE_1,true);
    }

    @Bean
    public Queue topicQueue2(){
        return new Queue(TOPIC_QUEUE_2,true);
    }


    /**
     * direct的exchange
     */
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(DIRECT_EXCHANGE);
    }


    /**
     * 按绑定规则将秒杀的队列绑定到direct Exchange
     */
    @Bean
    public Binding seckillBinding() {
        Binding binding = BindingBuilder.bind(seckillQueue()).to(directExchange()).with("seckill.key");
        return binding;
    }


    /**
     * 按绑定规则将队列绑定到direct exchange
     */
    @Bean
    public Binding directBinding() {
        Binding binding = BindingBuilder.bind(queue()).to(directExchange()).with("direct.key");
        return binding;
    }


    /**
     * 创建一个topic的exchange
     */
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE);
    }


    /**
     * 按绑定规则规则将数个队列绑定到Topic exchange
     */
    @Bean
    public Binding topicBinding1() {
        // 将topicQueue1绑定到topicExchange，接收routingKey为topic.key1的消息
        Binding binding = BindingBuilder.bind(topicQueue1()).to(topicExchange()).with("topic.key1");
        return binding;

        // 也可以如下的方式声明绑定规则
        // amqpAdmin.declareBinding(new Binding("adminQUNUE",Binding.DestinationType.QUEUE,"amqpAdminExchange","amqphKEY",null));
        // amqpAdmin.declareBinding(binding);
    }

    @Bean
    public Binding topicBinding2() {
        return BindingBuilder.bind(topicQueue2()).to(topicExchange()).with("topic.#");
    }


    /**
     * Fanout模式 交换机Exchange
     */
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(FANOUT_EXCHANGE);
    }

    /**
     * 制定fanout exchange上的绑定规则
     */
    @Bean
    public Binding FanoutBinding1() {
        return BindingBuilder.bind(topicQueue1()).to(fanoutExchange());
    }

    @Bean
    public Binding FanoutBinding2() {
        return BindingBuilder.bind(topicQueue2()).to(fanoutExchange());
    }

    /*
     * Header模式交换机Exchange
     */
    @Bean
    public HeadersExchange headersExchange() {
        return new HeadersExchange(HEADERS_EXCHANGE);
    }

    /*
     * 创建header队列
     */
    @Bean
    public Queue headerQueue1() {
        return new Queue(HEADER_QUEUE, true);
    }

    @Bean
    public Binding headerBinding() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("header1", "value1");
        map.put("header2", "value2");
        return BindingBuilder.bind(headerQueue1()).to(headersExchange()).whereAll(map).match();
    }


    /*
     * 将默认的SimpleMessageConverter改为自选的Converter
     */
    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }
}