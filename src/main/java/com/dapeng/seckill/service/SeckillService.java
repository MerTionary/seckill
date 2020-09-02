package com.dapeng.seckill.service;

import com.dapeng.seckill.bean.OrderInfo;
import com.dapeng.seckill.bean.SeckillOrder;
import com.dapeng.seckill.bean.SeckillUser;
import com.dapeng.seckill.bean.User;
import com.dapeng.seckill.redis.RedisService;
import com.dapeng.seckill.redis.SeckillKeyPrefix;
import com.dapeng.seckill.utils.MD5Util;
import com.dapeng.seckill.utils.UUIDUtil;
import com.dapeng.seckill.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

@Service
public class SeckillService {

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    RedisService redisService;

    // 用于生成验证码中的运算符
    private char[] ops = new char[]{'+', '-', '*'};

    // 减库存-下订单-写入秒杀订单(原子操作，事务)
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public OrderInfo seckill(SeckillUser user, GoodsVo goodsVo) {

        //减库存
        Boolean success = goodsService.reduceStock(goodsVo);
        if (success){
            //下订单-将订单记录插入到数据库和Redis
            return orderService.createOrder(user,goodsVo);
        }else {
            //redis中标记，是否卖完
            setGoodsOver(goodsVo.getId());
            return null;
        }

    }

    public long getSeckillResult(Long userId, long goodsId) {
        SeckillOrder seckillOrder = orderService.getSeckillOrderByUserIDGoodsId(userId, goodsId);
        if (seckillOrder!=null){//秒杀成功
            return seckillOrder.getOrderId();
        }else {
            Boolean isOver = getGoodsOver(goodsId);
            if (isOver){
                return -1;//卖完
            }else {
                return 0;//不是卖完，继续轮询
            }
        }
    }

    private void setGoodsOver(Long goodsId) {
        redisService.set(SeckillKeyPrefix.isGoodsOver,""+goodsId,true);
    }
    private Boolean getGoodsOver(long goodsId) {
        return redisService.existKey(SeckillKeyPrefix.isGoodsOver,""+goodsId);
    }

    public Boolean checkPath(String path, SeckillUser user, Long goodsId) {
        if (user==null||path==null) return false;
        String pathInRedis = redisService.get(SeckillKeyPrefix.seckillPath, "" + user.getId() + goodsId, String.class);
        return path.equals(pathInRedis);
    }

    public String createPath(Long id, Long goodsId) {
        String str = MD5Util.md5(UUIDUtil.uuid()+"RandomSalt");
        redisService.set(SeckillKeyPrefix.seckillPath,""+id+goodsId,str);
        return str;
    }

    /**
     * 创建验证码
     * @param user
     * @param goodsId
     * @return
     */
    public BufferedImage createVerifyCode(SeckillUser user, Long goodsId) {
        if (user==null||goodsId<=0) return null;

        // 验证码的宽高
        int width = 80;
        int height = 32;

        //create the image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        // set the background color
        g.setColor(new Color(0xDCDCDC));
        g.fillRect(0, 0, width, height);
        // draw the border
        g.setColor(Color.black);
        g.drawRect(0, 0, width - 1, height - 1);
        // create a random instance to generate the codes
        Random rdm = new Random();
        // make some confusion
        for (int i = 0; i < 50; i++) {
            int x = rdm.nextInt(width);
            int y = rdm.nextInt(height);
            g.drawOval(x, y, 0, 0);
        }
        // generate a random code
        String verifyCode = generateVerifyCode(rdm);
        g.setColor(new Color(0, 100, 0));
        g.setFont(new Font("Candara", Font.BOLD, 24));
        g.drawString(verifyCode, 8, 24);
        g.dispose();

        // 计算表达式结果值，并把把验证码值存到redis中
        int expResult = calc(verifyCode);
        redisService.set(SeckillKeyPrefix.seckillVerifyCode, user.getId() + "," + goodsId, expResult);
        //输出图片
        return image;
    }

    /**
     * 生成验证码，只含有+/-/*
     * 随机生成三个数字，然后生成表达式
     * @param rdm
     * @return 验证码中的数学表达式
     */
    private String generateVerifyCode(Random rdm) {
        int num1 = rdm.nextInt(10);
        int num2 = rdm.nextInt(10);
        int num3 = rdm.nextInt(10);
        char op1 = ops[rdm.nextInt(3)];
        char op2 = ops[rdm.nextInt(3)];
        String exp = "" + num1 + op1 + num2 + op2 + num3;
        return exp;
    }

    /**
     * 使用ScriptEngine计算验证码中的数学表达式exp的结果值
     * @param exp
     * @return
     */
    private int calc(String exp) {
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");
            return (Integer) engine.eval(exp);// 表达式计算
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }


    /**
     * 检验客户端传过来的验证码的计算结果
     * @param user
     * @param goodsId
     * @param verifyCode
     * @return
     */
    public boolean checkVerifyCode(SeckillUser user, long goodsId, int verifyCode) {
        if (user == null || goodsId <= 0) {
            return false;
        }

        // 从redis中获取存储的验证码计算结果
        Integer oldCode = redisService.get(SeckillKeyPrefix.seckillVerifyCode, user.getId() + "," + goodsId, Integer.class);
        if (oldCode == null || oldCode - verifyCode != 0) {// !!!!!!
            return false;
        }

        // 如果校验不成功，则说明校验码过期
        redisService.delete(SeckillKeyPrefix.seckillVerifyCode, user.getId() + "," + goodsId);
        return true;
    }

}