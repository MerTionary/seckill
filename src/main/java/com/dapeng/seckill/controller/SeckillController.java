package com.dapeng.seckill.controller;
import com.dapeng.seckill.access.AccessLimit;
import com.dapeng.seckill.bean.OrderInfo;
import com.dapeng.seckill.bean.SeckillOrder;
import com.dapeng.seckill.bean.SeckillUser;
import com.dapeng.seckill.rabbitmq.MQSender;
import com.dapeng.seckill.rabbitmq.SeckillMessage;
import com.dapeng.seckill.redis.GoodsKeyPrefix;
import com.dapeng.seckill.redis.RedisService;
import com.dapeng.seckill.redis.SeckillKeyPrefix;
import com.dapeng.seckill.result.CodeMsg;
import com.dapeng.seckill.result.Result;
import com.dapeng.seckill.service.GoodsService;
import com.dapeng.seckill.service.OrderService;
import com.dapeng.seckill.service.SeckillService;
import com.dapeng.seckill.service.SeckillUserService;
import com.dapeng.seckill.utils.MD5Util;
import com.dapeng.seckill.utils.UUIDUtil;
import com.dapeng.seckill.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@Controller
@RequestMapping("/seckill")
public class SeckillController implements InitializingBean {

    private static Logger logger = LoggerFactory.getLogger(LoginController.class);

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

    // 用于内存标记，标记库存是否为空，从而减少对redis的访问
    private HashMap<Long,Boolean> localOverMap = new HashMap<>();

    /*
     * 系统初始化的时候执行
     *     从数据库中将商品信息查询出来存入Redis（包含商品的秒杀信息seckill_goods和商品的基本信息goodsVo）
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> goodsVoList = goodsService.getGoodsVoList();
        if (goodsVoList==null) return;
        for (GoodsVo goodsVo : goodsVoList) {
            redisService.set(GoodsKeyPrefix.goodsStockListKeyPrefix,""+goodsVo.getId(),goodsVo.getStockCount());
            localOverMap.put(goodsVo.getId(),false);// 在系统启动时，标记库存不为空
        }
    }

    @AccessLimit(seconds=5, maxCount=5, needLogin=true)
    @GetMapping("/path")
    @ResponseBody
    public Result<String> getSeckillPath(ModelMap modelMap, SeckillUser user
            , @RequestParam("goodsId") Long goodsId
            , @RequestParam(value = "verifyCode", defaultValue = "0") int verifyCode) {

        //1、未登录直接返回到登录页
        if (user == null) return Result.error(CodeMsg.SESSION_ERROR);

        //2、校验验证码
        boolean check = seckillService.checkVerifyCode(user, goodsId, verifyCode);
        if (!check)
            return Result.error(CodeMsg.REQUEST_ILLEGAL);// 检验不通过，请求非法

        //3、检验通过，创建秒杀路径，并将路径参数存入redis
        String str = seckillService.createPath(user.getId(),goodsId);

        //4、向客户端回传随机生成的秒杀地址
        return Result.success(str);
    }


    @RequestMapping("/verifyCode")
    @ResponseBody
    public Result<String> getVerifyCode(HttpServletResponse response, SeckillUser user, @RequestParam("goodsId") Long goodsId) {

        //未登录直接返回到登录页
        if (user == null) return Result.error(CodeMsg.SESSION_ERROR);

        BufferedImage image = seckillService.createVerifyCode(user,goodsId);
        try {
            ServletOutputStream os = response.getOutputStream();
            ImageIO.write(image,"JPEG",os);
            os.flush();
            os.close();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error(CodeMsg.SECKILL_FAIL);
        }
    }



    // 本来根据Cookie从Redis里获取User是在controller方法中做，现在将其封装为UserArgumentResolver，直接将user封装到在controller的入参中，不需要传那么多参数了，完美！
    // Session 的作用就是为了标识一次会话，或者说确认一个用户。
    // 因为是域对象，并且可以在一次会话（一个用户的多次请求）期间共享数据，request.getSession().setAttribute("book",book);
    @PostMapping("/{path}/doSeckill")
    @ResponseBody
    public Result<Object> doSeckill(ModelMap modelMap, SeckillUser user
            , @RequestParam("goodsId") Long goodsId, @PathVariable("path") String path) {

        // 登录后，每次请求发送到到后台，根据cookie的token从redis查出来然后封装到入参内的。
        // 同时每次封装user时更新cookie的有效时间
        modelMap.addAttribute("user", user);

        // 未登录直接返回到登录页
        if (user == null) return Result.error(CodeMsg.SESSION_ERROR);

        // 验证path是否正确
        Boolean checked = seckillService.checkPath(path,user,goodsId);
        if (!checked) return Result.error(CodeMsg.REQUEST_ILLEGAL);// 请求非法

        // 通过内存标记，减少对redis的访问，秒杀未结束才继续访问redis
        Boolean over = localOverMap.get(goodsId);
        if (over) return Result.error(CodeMsg.STOCK_EMPTY);

        //1、预减库存Redis
        // （这里有个问题，在redis预减库存，入队，但实际队列中的秒杀可能失败，mysql还有库存时，而redis已经没有了，出现错误！）
        Long decrStock = redisService.decr(GoodsKeyPrefix.goodsStockListKeyPrefix, "" + goodsId);
        System.out.println("goodsId为："+goodsId+" 其预减后为"+decrStock);
        if (decrStock< 0) {
            localOverMap.put(goodsId,true);
            return Result.error(CodeMsg.STOCK_EMPTY);
        }

        //2、判断是否已秒杀到了，防止超卖(createOrder是把订单信息同时插入了mysql和redis，但查的时候只从redis查)（Redis）
        SeckillOrder seckillOrder = orderService.getSeckillOrderByUserIDGoodsId(user.getId(), goodsId);
        if (seckillOrder != null) {

            //既然已经有了就把redis预减加回去！
            Long incrStock = redisService.incr(GoodsKeyPrefix.goodsStockListKeyPrefix, "" + goodsId);
            System.out.println("goodsId为："+goodsId+" 其回滚后为"+incrStock);

            return Result.error(CodeMsg.SECKILL_REPEAT);
        }

        //3、创建MQ秒杀对象，并将秒杀请求入队
        SeckillMessage message = new SeckillMessage();
        message.setUser(user);
        message.setGoodsId(goodsId);
        mqSender.sendSeckillMesssage(message);

        //4、返回前端-排队中
        return Result.success(0);

        /*
        //1、判断库存(从mysql查)
        GoodsVo goodsVo = goodsService.getGoodsVoByGoodsId(goodsId);//假如10个商品，某用户同时发出了两个请求req1 req2进来（都没有查到秒杀记录，然后秒杀到了两个产品）
        Integer stockCount = goodsVo.getStockCount();
        if (stockCount <= 0) return Result.error(CodeMsg.STOCK_EMPTY);


        //2、判断是否当前用户是否已经秒杀到了，防止超卖(createOrder是把订单信息同时插入了mysql和redis，但查的时候只从redis查)
        SeckillOrder seckillOrder = orderService.getSeckillOrderByUserIDGoodsId(user.getId(), goodsId);

        if (seckillOrder != null) return Result.error(CodeMsg.SECKILL_REPEAT);

        //3、减库存-下订单-写入秒杀订单(原子操作，事务)
        OrderInfo orderInfo = seckillService.seckill(user,goodsVo);
        return Result.success(orderInfo);
        */

    }

    /**
     * 用于返回用户秒杀的结果
     *
     * @param model
     * @param user
     * @param goodsId
     * @return
     *      orderId：成功
     *          -1：秒杀失败
     *          0： 排队中
     */
    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public Result<Long> seckillResult(Model model, SeckillUser user,
                                      @RequestParam("goodsId") long goodsId) {
        model.addAttribute("user", user);
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        long result = seckillService.getSeckillResult(user.getId(), goodsId);
        return Result.success(result);
    }

}