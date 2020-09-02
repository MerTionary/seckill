package com.dapeng.seckill.dao;

import com.dapeng.seckill.bean.SeckillGoods;
import com.dapeng.seckill.vo.GoodsVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Mapper
public interface GoodsDao {

    @Select("select g.*, sg.seckill_price, sg.stock_count,sg.start_date, sg.end_date from seckill_goods sg left join goods g on sg.goods_id = g.id")
    public List<GoodsVo> getGoodsVoList();

    @Select("select g.*, sg.seckill_price, sg.stock_count,sg.start_date, sg.end_date from seckill_goods sg left join goods g on sg.goods_id = g.id where g.id=#{goodsId}")
    public GoodsVo getGoodsByGoodsId(@Param("goodsId") Long goodsId);


    //mysql本身会对这条记录加个锁，不会出现两个线程同时更新同一条记录的情况（行锁）
    @Update("update seckill_goods set stock_count = stock_count-1 where goods_id = #{goodsId} and stock_count>0")
    Integer reduceStock(SeckillGoods seckillGoods);
}