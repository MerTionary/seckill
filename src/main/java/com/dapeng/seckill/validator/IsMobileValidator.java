package com.dapeng.seckill.validator;

import com.dapeng.seckill.utils.ValidatorUtil;
import com.sun.org.apache.xerces.internal.impl.dv.ValidatedInfo;
import org.springframework.validation.annotation.Validated;
import org.thymeleaf.util.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
/**
 * 真正用户手机号码检验的工具，会被注解@isMobile所使用
 * 这个类需要实现javax.validation.ConstraintValidator，否则不能被@Constraint参数使用
 */
public class IsMobileValidator implements ConstraintValidator<IsMobile, String> {

    // 用于获取检验字段是否可以为空
    private boolean required = false;

    //初始化方法，获取注解
    @Override
    public void initialize(IsMobile constraintAnnotation) {
        required=constraintAnnotation.required();
    }

    /**
     * 用于检验字段是否合法
     *
     * @param value   待校验的字段
     * @param context
     * @return 字段检验结果
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        // 如果所检验字段可以为空
        if (required){
            return ValidatorUtil.isMobile(value);
        }else{
            if (StringUtils.isEmpty(value)) {
                return true;
            }else {
                return ValidatorUtil.isMobile(value);
            }
        }
    }
}