package com.dapeng.seckill.controller;

import com.dapeng.seckill.bean.SeckillUser;
import com.dapeng.seckill.redis.GoodsKeyPrefix;
import com.dapeng.seckill.redis.RedisService;
import com.dapeng.seckill.result.Result;
import com.dapeng.seckill.service.GoodsService;
import com.dapeng.seckill.service.SeckillUserService;
import com.dapeng.seckill.vo.GoodsDetailVo;
import com.dapeng.seckill.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/goods")
public class GoodsController {

    private static Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    SeckillUserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    ThymeleafViewResolver thymeleafViewResolver;

    @Autowired
    ApplicationContext applicationContext;
    /*
        QPS=900
        请求数：5000*10
        异常比例：69%

        QPS=1919
     */
    // 列表页
    // 本来根据Cookie从Redis里获取User是在controller方法中做，现在将其封装为UserArgumentResolver，直接将user封装到在controller的入参中，不需要传那么多参数了，完美！
    // Session 的作用就是为了标识一次会话，或者说确认一个用户。因为是域对象，并且可以在一次会话（一个用户的多次请求）期间共享数据，request.getSession().setAttribute("book",book);
    @RequestMapping(value = "/toList", produces = "text/html")// produces表明：这个请求会返回text/html媒体类型的数据
    public String toList(HttpServletRequest request, HttpServletResponse response,
                         ModelMap modelMap, SeckillUser user) {

        // 登录后，每次请求到后台，根据cookie的token从redis查出来然后封装到入参内的。同时每次封装user时更新cookie的有效时间
        modelMap.addAttribute("user", user);
        // return "goodsList";

        //1、从redis取出html缓存
        String html = redisService.get(GoodsKeyPrefix.goodsListKeyPrefix, "", String.class);

        //2、html缓存不为空，返回该html
        if (!StringUtils.isEmpty(html)) return html;

        //3、查询秒杀商品列表，用于手动渲染时将商品数据填充到页面
        List<GoodsVo> goodsList = goodsService.getGoodsVoList();
        modelMap.addAttribute("goodsList",goodsList);

        //3、手动渲染页面
        // (第一个参数为渲染的html文件名，第二个为web上下文：里面封装了web应用的上下文)
        WebContext webContext = new WebContext(request,response,request.getServletContext(),request.getLocale(),modelMap);
        html = thymeleafViewResolver.getTemplateEngine().process("goodsList", webContext);

        if (!StringUtils.isEmpty(html)){
            // 如果html文件不为空，则将页面缓存在redis中
            // 设置页面缓存的有效期为60s（在redis中）
            redisService.set(GoodsKeyPrefix.goodsListKeyPrefix,"", html);
        }
        System.out.println("--------------手动渲染");
        return html;
    }

    //静态化-详情页（因为这个页面静态化的商品详情页，虽然静态化了页面，数据每次都查询mysql...而原先return html，页面和数据都缓存）
    @RequestMapping(value = "/toDetailStatic/{goodsId}")
    public Result<GoodsDetailVo> toDetailStatic(HttpServletRequest request, HttpServletResponse response,
                                          ModelMap modelMap, SeckillUser user, @PathVariable("goodsId") Long goodsId) {

        //通过商品id再数据库查询
        GoodsVo goodsVo = goodsService.getGoodsVoByGoodsId(goodsId);

        long startTime = goodsVo.getStartDate().getTime();
        long endTime = goodsVo.getEndDate().getTime();
        long nowTime = System.currentTimeMillis();

        // 秒杀状态; 0: 秒杀未开始，1: 秒杀进行中，2: 秒杀已结束
        int seckillStatus = 0;
        // 秒杀剩余时间
        int remainSeconds = 0;
        if (nowTime < startTime){//秒杀未开始
            seckillStatus = 0;
            remainSeconds = (int) (startTime - nowTime)/1000;
        }else if(nowTime>endTime){//秒杀结束
            seckillStatus = 2;
            remainSeconds = -1;
        }else {//秒杀进行中
            seckillStatus = 1;
            remainSeconds = 0;
        }

        GoodsDetailVo goodsDetailVo = new GoodsDetailVo();
        goodsDetailVo.setGoods(goodsVo);
        goodsDetailVo.setUser(user);
        goodsDetailVo.setSeckillStatus(seckillStatus);
        goodsDetailVo.setRemainSeconds(remainSeconds);
        return Result.success(goodsDetailVo);
    }



    //详情页
    @RequestMapping(value = "/toDetail/{goodsId}", produces = "text/html")
    public String toDetail(HttpServletRequest request, HttpServletResponse response,
                                          ModelMap modelMap, SeckillUser user, @PathVariable("goodsId") Long goodsId) {

        // 这个user是登录后根据cookie的token从redis查出来然后封装到入参内的。同时每次封装user时更新cookie的有效时间
        modelMap.addAttribute("user", user);

        //1、从redis取出html缓存
        String html = redisService.get(GoodsKeyPrefix.goodsDetailKeyPrefix, ""+goodsId, String.class);

        //2、html缓存不为空，返回该html
        if (!StringUtils.isEmpty(html)) return html;

        //商品详情存入域对象
        GoodsVo goodsVo = goodsService.getGoodsVoByGoodsId(goodsId);
        modelMap.addAttribute("goods",goodsVo);

        long startTime = goodsVo.getStartDate().getTime();
        long endTime = goodsVo.getEndDate().getTime();
        long nowTime = System.currentTimeMillis();

        // 秒杀状态; 0: 秒杀未开始，1: 秒杀进行中，2: 秒杀已结束
        int seckillStatus = 0;
        // 秒杀剩余时间
        int remainSeconds = 0;
        if (nowTime < startTime){//秒杀未开始
            seckillStatus = 0;
            remainSeconds = (int) (startTime - nowTime)/1000;
        }else if(nowTime>endTime){//秒杀结束
            seckillStatus = 2;
            remainSeconds = -1;
        }else {//秒杀进行中
            seckillStatus = 1;
            remainSeconds = 0;
        }
        modelMap.addAttribute("seckillStatus",seckillStatus);
        modelMap.addAttribute("remainSeconds",remainSeconds);

        //3、手动渲染页面
        // (第一个参数为渲染的html文件名，第二个为web上下文：里面封装了web应用的上下文)
        WebContext webContext = new WebContext(request,response,request.getServletContext(),request.getLocale(),modelMap);
        html = thymeleafViewResolver.getTemplateEngine().process("goodsDetail", webContext);

        if (!StringUtils.isEmpty(html)){
            // 如果html文件不为空，则将页面缓存在redis中
            // 设置页面缓存的有效期为60s（在redis中）
            redisService.set(GoodsKeyPrefix.goodsDetailKeyPrefix,""+goodsId, html);
        }
        GoodsDetailVo goodsDetailVo = new GoodsDetailVo();
        goodsDetailVo.setGoods(goodsVo);
        goodsDetailVo.setUser(user);
        goodsDetailVo.setSeckillStatus(seckillStatus);
        return html;
    }
}

    // @RequestMapping("/toList")
    // public String toList(HttpServletResponse response, ModelMap modelMap,
    //                      // @CookieValue(value = SeckillUserService.COOKIE_NAME_TOKEN, required = false) String cookieToken,
    //                      // @RequestParam(value = SeckillUserService.COOKIE_NAME_TOKEN, required = false) String paramToken,
    //                      SeckillUser user) {
    //
    //     // if (StringUtils.isEmpty(cookieToken)&&StringUtils.isEmpty(paramToken)) return "login";
    //     // String token = StringUtils.isEmpty(cookieToken)?paramToken:cookieToken;
    //     // SeckillUser user =  userService.getSeckillUserByToken(response, token);
    //     System.out.println("返回到goodList页面前获取的user：" + user);
    //
    //     modelMap.addAttribute("user", user);
    //
    //     return "goodsList";
    // }