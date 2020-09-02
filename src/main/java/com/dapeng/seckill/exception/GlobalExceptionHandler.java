package com.dapeng.seckill.exception;


import com.dapeng.seckill.result.CodeMsg;
import com.dapeng.seckill.result.Result;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


/**
 * 定制错误的json数据
 */
//@ControllerAdvice顾名思义，这是一个增强的 Controller（底层使用方法拦截的方式完成，和AOP一样）,可以实现三个方面的功能：
//  全局异常处理
//    需要配合@ExceptionHandler使用,当将异常抛到controller时,可以对异常进行统一处理,规定返回的json格式 或 跳转到一个错误页面
//  全局数据绑定
//  全局数据预处理

@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)// 这个注解用指定这个方法对何种异常处理（这里默认所有异常都用这个方法处理）
    public Result<String> exceptionHandler(HttpServletRequest request, Exception exception) {
        exception.printStackTrace();

        // 如果所拦截的异常是自定义的GlobalException，这按自定义异常的处理方式处理，否则按默认方式处理（处理BindException和其他的异常）
        if (exception instanceof GlobalException) {
            GlobalException e = (GlobalException) exception;
            return Result.error(e.getCodeMsg());
        } else if (exception instanceof BindException) {
            BindException bindException = (BindException) exception;
            List<ObjectError> errorList = bindException.getAllErrors();
            ObjectError error = errorList.get(0);// 这里只获取了第一个错误对象
            String msg = error.getDefaultMessage();// 获取其中的异常信息
            return Result.error(CodeMsg.BIND_ERROR.fillArgs(msg));
        } else {
            return Result.error(CodeMsg.SERVER_ERROR);//如果不是绑定异常，返回一个通用的server异常
        }
    }


}