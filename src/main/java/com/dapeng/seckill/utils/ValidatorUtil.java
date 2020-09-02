package com.dapeng.seckill.utils;

import org.thymeleaf.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidatorUtil {

    //利用正则表达式校验手机号码格式
    private static final Pattern mobile_pattern = Pattern.compile("1\\d{10}");

    public static boolean isMobile(String mobile){
        if (StringUtils.isEmpty(mobile)) return false;
        Matcher matcher = mobile_pattern.matcher(mobile);
        return matcher.matches();
    }
}