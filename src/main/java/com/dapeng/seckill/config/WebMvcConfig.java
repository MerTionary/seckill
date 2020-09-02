package com.dapeng.seckill.config;

import com.dapeng.seckill.access.AccessLimitHandlerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

//@EnableWebMvc //所有的SpringMVC的自动配置都失效了
//编写一个配置类@Configuration，是WebMvcConfigurer类型，统一扩展各种webmvc配置，效果：SpringMVC的自动配置和我们的扩展配置都会起作用；
@Configuration
public class WebMvcConfig implements WebMvcConfigurer{

    //造成null的原因是因为拦截器加载是在SpringContext创建之前完成的，所以在拦截器中注入实体自然就为null。
    @Bean
    public AccessLimitHandlerInterceptor getInterceptor(){
        return new AccessLimitHandlerInterceptor();
    }


    @Autowired
    UserArgumentResolver userArgumentResolver;

    /**
     * 注册自定义的AccessLimit拦截器
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(getInterceptor());
    }

    /**
     * 注册自定义的参数解析器到MVC配置
     * @param resolvers
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userArgumentResolver);
    }
}