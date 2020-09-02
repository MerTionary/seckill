package com.dapeng.seckill.config;

import com.dapeng.seckill.access.UserContext;
import com.dapeng.seckill.bean.SeckillUser;
import com.dapeng.seckill.service.SeckillUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * 解析请求，并将请求的参数设置到入参中
 */
@Service
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    SeckillUserService userService;

    /**
     * 当请求参数为SeckillUser时，使用这个解析器处理
     * 客户端的请求到达某个Controller的方法时，判断这个方法入参是否为SeckillUser，
     * 如果是，则这个SeckillUser参数对象通过下面的resolveArgument()方法获取，
     * 然后，该Controller方法继续往下执行时所看到的SeckillUser对象就是在这里的resolveArgument()方法处理过的对象
     *
     *      相当于本来根据Cookie从Redis里获取User是在controller方法中做，现在将其封装为UserArgumentResolver，直接将user封装到在controller的入参中，完美！
     *
     * @param methodParameter
     * @return
     */
    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        Class<?> type = methodParameter.getParameterType();
        return type == SeckillUser.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        // 从ThreadLocal中取user，因为拦截器首先执行。
        // return UserContext.getUser();

        // 获取请求和响应对象
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);

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