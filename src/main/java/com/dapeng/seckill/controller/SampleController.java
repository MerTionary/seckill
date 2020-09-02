package com.dapeng.seckill.controller;

import com.dapeng.seckill.bean.User;
import com.dapeng.seckill.rabbitmq.MQSender;
import com.dapeng.seckill.redis.RedisService;
import com.dapeng.seckill.redis.UserKeyPrefix;
import com.dapeng.seckill.result.CodeMsg;
import com.dapeng.seckill.result.Result;
import com.dapeng.seckill.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/demo")
public class SampleController {

    @Autowired
    UserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    MQSender mqSender;

    @RequestMapping("/")
    @ResponseBody
    String home(){
        return "HelloWorld";
    }

    //1.rest api的json输出
    // 2.页面
    @RequestMapping("/hello")
    @ResponseBody
    public Result<String> hello() {
        return Result.success("hello,success");
        // return new Result(0, "success", "hello,success");
    }

    @RequestMapping("/helloError")
    @ResponseBody
    public Result<String> helloError() {
        return Result.error(CodeMsg.SERVER_ERROR);
        //return new Result(500102, "XXX");
    }

    @RequestMapping("/thymeleaf")
    public String  thymeleaf(Model model) {
        model.addAttribute("name", "Dapeng");
        return "hello";
    }

    @GetMapping("/db/get/{id}")
    @ResponseBody
    public Result<User> dbGet(@PathVariable("id") Integer id) {
        User user = userService.getUserById(id);
        return Result.success(user);
    }

    @GetMapping("/db/tx")
    @ResponseBody
    public Result<Boolean> dbTx() {
        userService.tx();
        return Result.success(true);
    }


    @GetMapping("/redis/get")
    @ResponseBody
    public Result<User> redisGet() {
        User user = redisService.get(UserKeyPrefix.userKeyPrefixById, "" + 1, User.class);
        return Result.success(user);
    }

    @GetMapping("/redis/set")
    @ResponseBody
    public Result<Boolean> redisSet() {
        User user = new User();
        user.setId(3);
        user.setName("大鹏");
        boolean set = redisService.set(UserKeyPrefix.userKeyPrefixById,""+1, user);//Prefix;
        return Result.success(true);
    }

    @GetMapping("/mq")
    @ResponseBody
    public Result<String> mq() {
        mqSender.send("helloMQ");
        return Result.success("helloMQ");
    }


    @GetMapping("/mq/topic")
    @ResponseBody
    public Result<String> mqTopic() {
        mqSender.sendTopic("mqTopic");
        return Result.success("mqTopic");
    }

    @GetMapping("/mq/fanout")
    @ResponseBody
    public Result<String> mqFanout() {
        mqSender.sendTopic("mqFanout");
        return Result.success("mqFanout");
    }
    @GetMapping("/mq/direct")
    @ResponseBody
    public Result<String> mqDirect() {
        mqSender.sendDirect("mqDirect");
        return Result.success("mqDirect");
    }
}