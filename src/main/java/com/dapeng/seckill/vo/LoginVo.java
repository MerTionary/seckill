package com.dapeng.seckill.vo;

import com.dapeng.seckill.validator.IsMobile;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

/**
 * VO（value object）是值对象，精确点讲它是业务对象，是存活在业务层的，是业务逻辑使用的，
 * 它存活的目的就是为数据提供一个生存的地方。VO的属性是根据当前业务的不同而不同的，
 * 也就是说，它的每一个属性都一一对应当前业务逻辑所需要的数据的名称
 *      用于接收客户端请求中的表单数据
 *      使用JSR303完成参数校验
 */
public class LoginVo {

    //需要校验的参数前加注解，不同的注解有不同的校验规则

    @NotNull
    @IsMobile
    private String mobile;

    @NotNull
    @Length(min = 32)
    private String password;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "LoginVo{" +
                "mobile='" + mobile + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}