package com.dapeng.seckill.service;

import com.dapeng.seckill.bean.SeckillGoods;
import com.dapeng.seckill.dao.GoodsDao;
import com.dapeng.seckill.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoodsService {

    @Autowired
    GoodsDao goodsDao;

    public List<GoodsVo> getGoodsVoList(){
        return goodsDao.getGoodsVoList();
    }

    public GoodsVo getGoodsVoByGoodsId( Long goodsId) {
        return goodsDao.getGoodsByGoodsId(goodsId);
    }

    public Boolean reduceStock(GoodsVo goodsVo) {
        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setGoodsId(goodsVo.getId());
        //直接传个goodsId不香吗？
        Integer result = goodsDao.reduceStock(seckillGoods);
        //假设库存只有1个了，两个人同时调用就出问题了
        //  解决：sql加>0的判断条件，and stock_count>0。mysql本身会对这条记录加个锁，不会出现两个线程同时更新同一条记录的情况（行锁）

        return result > 0;
    }
}