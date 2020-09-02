package com.dapeng.seckill.dao;

import com.dapeng.seckill.bean.OrderInfo;
import com.dapeng.seckill.bean.SeckillOrder;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Component;

@Mapper
@Component
public interface OrderDao {

    @Select("select * from seckill_order where user_id=#{userId} and goods_id=#{goodsId}")
    SeckillOrder getSeckillOrderByUserIDGoodsId(@Param("userId") Long userId, @Param("goodsId")Long goodsId);

    /**
     * 将订单信息插入order_info表中
     * @param orderInfo 订单信息
     * @return 插入成功的订单信息id
     */
    @Insert("INSERT INTO order_info (user_id, goods_id, goods_name, goods_count, goods_price, order_channel, status, create_date)"
            + "VALUES (#{userId}, #{goodsId}, #{goodsName}, #{goodsCount}, #{goodsPrice}, #{orderChannel},#{status},#{createDate} )")
    // 查询出插入订单信息的表id，并返回
    @SelectKey(keyColumn = "id", keyProperty = "id", resultType = long.class, before = false, statement = "SELECT last_insert_id()")
    Long insertOrderInfo(OrderInfo orderInfo);

    /**
     * 将秒杀订单信息插入到seckill_order中
     * @param seckillOrder 秒杀订单
     */
    @Insert("INSERT INTO seckill_order(user_id, order_id, goods_id) VALUES (#{userId}, #{orderId}, #{goodsId})")
    void insertSeckillOrder(SeckillOrder seckillOrder);



    /**
     * 获取订单信息
     * @param orderId
     * @return
     */
    @Select("select * from order_info where id = #{orderId}")
    OrderInfo getOrderById(long orderId);
}
