package com.dapeng.seckill.rabbitmq;

import com.dapeng.seckill.bean.SeckillUser;

public class SeckillMessage {

    private SeckillUser user;

    private Long goodsId;

    public SeckillUser getUser() {
        return user;
    }

    public void setUser(SeckillUser user) {
        this.user = user;
    }

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }
}