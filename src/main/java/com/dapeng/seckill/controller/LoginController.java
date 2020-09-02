package com.dapeng.seckill.controller;

import com.dapeng.seckill.bean.User;
import com.dapeng.seckill.redis.RedisService;
import com.dapeng.seckill.redis.UserKeyPrefix;
import com.dapeng.seckill.result.CodeMsg;
import com.dapeng.seckill.result.Result;
import com.dapeng.seckill.service.SeckillUserService;
import com.dapeng.seckill.service.UserService;
import com.dapeng.seckill.utils.UUIDUtil;
import com.dapeng.seckill.utils.ValidatorUtil;
import com.dapeng.seckill.vo.LoginVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/login")
public class LoginController {

    private static Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    SeckillUserService userService;

    @Autowired
    RedisService redisService;

    @RequestMapping("/toLogin")
    public String toLogin() {
        return "login";
    }

    /**
     * 登录功能·
     * @param loginVo
     * @return
     */
    @RequestMapping("/doLogin")
    @ResponseBody
    public Result<Boolean> doLogin(HttpServletResponse response, @Validated LoginVo loginVo) {
        //日志打印
        logger.info(loginVo.toString());
        // 参数校验
        // 异常在GlobalExceptionHandler里返回Result.error(xxx)给前端

        //登录
        Boolean login = userService.login(response,loginVo);
        return Result.success(true);
    }
}