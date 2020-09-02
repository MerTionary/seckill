package com.dapeng.seckill.access;

import com.alibaba.fastjson.JSON;
import com.dapeng.seckill.bean.SeckillUser;
import com.dapeng.seckill.redis.AccessKeyPrefix;
import com.dapeng.seckill.redis.RedisService;
import com.dapeng.seckill.result.CodeMsg;
import com.dapeng.seckill.result.Result;
import com.dapeng.seckill.service.SeckillUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

@Component
public class AccessLimitHandlerInterceptor implements HandlerInterceptor {

    @Autowired
    SeckillUserService userService;

    // 拦截器加载的时间点在SpringContext创建之前，所以在拦截器中注入自然为null
    @Autowired
    RedisService redisService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 指明拦截的是方法
        if(handler instanceof HandlerMethod){

            SeckillUser user = this.getSeckillUser(request, response);// 获取用户对象
            UserContext.setUser(user);// 保存用户到ThreadLocal，这样，同一个线程访问的是同一个用户

            // 获取标注了@AccessLimit的方法，没有注解，则直接返回
            HandlerMethod handlerMethod = (HandlerMethod) handler;

            // 如果没有添加@AccessLimit注解，直接放行（true）
            AccessLimit accessLimit = handlerMethod.getMethodAnnotation(AccessLimit.class);
            if (accessLimit==null) return true;

            // 获取注解的元素值
            int seconds = accessLimit.seconds();
            int maxCount = accessLimit.maxCount();
            boolean needLogin = accessLimit.needLogin();

            String key = request.getRequestURI();

            if (needLogin) {
                if (user == null) {
                    this.render(response, CodeMsg.SESSION_ERROR);
                    return false;
                }
                key += "_" + user.getId();
            }

            // 设置过期时间
            AccessKeyPrefix accessKeyPrefix = AccessKeyPrefix.withExpire(seconds);
            // 在redis中存储的访问次数的key为请求的URI
            Integer count = redisService.get(accessKeyPrefix, key, Integer.class);
            // 第一次重复点击秒杀
            if (count == null) {
                redisService.set(accessKeyPrefix, key, 1);
                // 点击次数为未达最大值
            } else if (count < maxCount) {
                redisService.incr(accessKeyPrefix, key);
                // 点击次数已满
            } else {
                this.render(response, CodeMsg.ACCESS_LIMIT_REACHED);
                return false;
            }
        }
        return true;
    }

    /**
     * 点击次数已满后，向客户端反馈一个“频繁请求”提示信息
     *
     * @param response
     * @param sessionError
     * @throws IOException
     */
    private void render(HttpServletResponse response, CodeMsg sessionError) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        OutputStream os = response.getOutputStream();
        String jsonString = JSON.toJSONString(Result.error(sessionError));
        os.write(jsonString.getBytes("UTF-8"));
        os.flush();
        os.close();
    }

    /**
     * 和UserArgumentResolver功能类似，用于解析拦截的请求，获取SeckillUser对象
     * @param request
     * @param response
     * @return
     */
    private SeckillUser getSeckillUser(HttpServletRequest request, HttpServletResponse response){

        // 从请求对象中获取token（token可能有两种方式从客户端返回，1：通过url的参数，2：通过set-Cookie字段）
        String paramToken = request.getParameter(SeckillUserService.COOKIE_NAME_TOKEN);
        String cookieToken = getCookieValue(request, SeckillUserService.COOKIE_NAME_TOKEN);

        // 判断是哪种方式返回的token，并由该种方式获取token（cookie）
        if(StringUtils.isEmpty(paramToken)&&StringUtils.isEmpty(cookieToken)) return null;
        String token = StringUtils.isEmpty(cookieToken)?paramToken:cookieToken;

        // 通过token就可以在redis中查出该token对应的用户对象
        // 同时每次封装user时更新cookie的有效时间
        return userService.getSeckillUserByToken(response, token);
    }

    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();

        // null判断，否则并发时会发生异常
        if(cookies==null|| cookies.length<=0) return null;

        for (Cookie cookie: cookies) {
            if (cookie.getName().equals(cookieName)) return cookie.getValue();
        }
        return null;
    }
}