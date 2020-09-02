package com.dapeng.seckill.vo;

import com.dapeng.seckill.bean.SeckillUser;

public class GoodsDetailVo {

    // 秒杀状态; 0: 秒杀未开始，1: 秒杀进行中，2: 秒杀已结束
    private int seckillStatus = 0;
    // 秒杀剩余时间
    private int remainSeconds = 0;
    private GoodsVo goods;
    private SeckillUser user;

    public int getSeckillStatus() {
        return seckillStatus;
    }

    public void setSeckillStatus(int seckillStatus) {
        this.seckillStatus = seckillStatus;
    }

    public int getRemainSeconds() {
        return remainSeconds;
    }

    public void setRemainSeconds(int remainSeconds) {
        this.remainSeconds = remainSeconds;
    }

    public GoodsVo getGoods() {
        return goods;
    }

    public void setGoods(GoodsVo goods) {
        this.goods = goods;
    }

    public SeckillUser getUser() {
        return user;
    }

    public void setUser(SeckillUser user) {
        this.user = user;
    }
}