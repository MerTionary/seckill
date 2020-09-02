package com.dapeng.seckill.controller;

import com.dapeng.seckill.bean.SeckillUser;
import com.dapeng.seckill.redis.RedisService;
import com.dapeng.seckill.result.Result;
import com.dapeng.seckill.service.GoodsService;
import com.dapeng.seckill.service.SeckillUserService;
import com.dapeng.seckill.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/user")
public class UserController {

    // 列表页
    // 本来根据Cookie从Redis里获取User是在controller方法中做，现在将其封装为UserArgumentResolver，直接将user封装到在controller的入参中，不需要传那么多参数了，完美！
    // Session 的作用就是为了标识一次会话，或者说确认一个用户。因为是域对象，并且可以在一次会话（一个用户的多次请求）期间共享数据，request.getSession().setAttribute("book",book);

    @RequestMapping("/info")
    @ResponseBody
    public Result<SeckillUser> info(ModelMap modelMap, SeckillUser user) {

        //登录后，每次请求到后台，根据cookie的token从redis查出来然后封装到入参内的。
        // 同时每次封装user时更新cookie的有效时间
        modelMap.addAttribute("user", user);

        return Result.success(user);
    }

}
