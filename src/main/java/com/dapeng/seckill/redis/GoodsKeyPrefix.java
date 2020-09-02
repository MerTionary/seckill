package com.dapeng.seckill.redis;

public class GoodsKeyPrefix extends BaseKeyPrefix{

    public GoodsKeyPrefix(int expireSecond, String prefix) {
        super(expireSecond,prefix);
    }
    public static GoodsKeyPrefix goodsListKeyPrefix = new GoodsKeyPrefix(60, "gl");
    public static GoodsKeyPrefix goodsDetailKeyPrefix = new GoodsKeyPrefix(60, "gd");
    public static GoodsKeyPrefix goodsStockListKeyPrefix = new GoodsKeyPrefix(0, "gsl");
}