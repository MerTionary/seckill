package com.dapeng.seckill.service;

import com.dapeng.seckill.bean.SeckillUser;
import com.dapeng.seckill.dao.SeckillUserDao;
import com.dapeng.seckill.exception.GlobalException;
import com.dapeng.seckill.redis.RedisService;
import com.dapeng.seckill.redis.SeckillUserKeyPrefix;
import com.dapeng.seckill.result.CodeMsg;
import com.dapeng.seckill.utils.MD5Util;
import com.dapeng.seckill.utils.UUIDUtil;
import com.dapeng.seckill.vo.LoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@Service
public class SeckillUserService {

    public static final String COOKIE_NAME_TOKEN = "token";

    @Autowired
    SeckillUserDao seckillUserDao;

    @Autowired
    RedisService redisService;

    public SeckillUser getSeckillUserById(Long id){

        //1、取缓存中的user对象
        SeckillUser user = redisService.get(SeckillUserKeyPrefix.seckillUserKeyPrefixById, "" + id, SeckillUser.class);
        if (user!=null) return user;

        //2、缓存中没有user对象，则查询数据库，同时将该user存入redis，再返回user，
        user = seckillUserDao.getSkillUserById(id);
        if (user!=null) redisService.set(SeckillUserKeyPrefix.seckillUserKeyPrefixById,"" + id, user);

        return user;
    }

    /**
     * 万一用户信息更新了怎么办，比如密码、手机号等等，那么一定在update和delete方法内更新user缓存！
     * @param id
     * @param formPwd
     * @return
     */
    public Boolean updateSeckillUser(String token, Long id, String formPwd){

        //1、取user(因为登陆了才能改密码，所以redis里缓存了user)
        SeckillUser user = getSeckillUserById(id);
        if (user==null) throw new GlobalException(CodeMsg.MOBILE_NULL);

        //2、更新数据库
        SeckillUser toBeUpdateedUser = new SeckillUser();
        toBeUpdateedUser.setId(id);
        toBeUpdateedUser.setPassword(MD5Util.formPwd2DBPwd(formPwd,user.getSalt()));
        Boolean result =  seckillUserDao.updateUser(user);

        //3、更新缓存(token和user都要更新，先删除再添加)
          // 删除对象缓存（查询对象时再添加，好比实际中修改密码后重新登陆）
        redisService.delete(SeckillUserKeyPrefix.seckillUserKeyPrefixById,""+id);
          // 设置新的token缓存
        user.setPassword(toBeUpdateedUser.getPassword());
        redisService.set(SeckillUserKeyPrefix.token,token,user);// 为新的用户数据重新生成缓存（token不变只是覆盖掉了）

        return true;
    }

    /**
     * 用户登录, 要么处理成功返回true，否则会抛出全局异常
     * 抛出的异常信息会被全局异常接收，全局异常会将异常信息传递到全局异常处理器
     * 自定义的GlobalExceptionHandler默认会对所有的异常都进行处理
     * @param loginVo
     * @return
     */
    public Boolean login(HttpServletResponse response, LoginVo loginVo) {

        if (loginVo==null) throw new GlobalException(CodeMsg.SERVER_ERROR);
        String mobile = loginVo.getMobile();
        String password = loginVo.getPassword();

        //从数据库中查询user的信息(Redis查询用户信息)
        SeckillUser user = getSeckillUserById(Long.parseLong(mobile));

        //判断手机号是否存在
        if (user==null) throw new GlobalException(CodeMsg.MOBILE_NULL);

        //验证密码
        String dbPwd = user.getPassword();
        String saltDB = user.getSalt();
        String calPwd = MD5Util.formPwd2DBPwd(password, saltDB);
        if (!dbPwd.equals(calPwd)) throw new GlobalException(CodeMsg.PASSWORD_ERROR);

        //生成cookie：
        //      将token和user存入redis；
        //      将token以cookie的形式返给客户端。
        String token = UUIDUtil.uuid();
        addCookie(response, token, user);
        return true;
    }

    public SeckillUser getSeckillUserByToken(HttpServletResponse response, String token) {
        if (StringUtils.isEmpty(token)) return null;
        SeckillUser user = redisService.get(SeckillUserKeyPrefix.token, token, SeckillUser.class);

        // 同时更新cookie以延长session有效期
        if (user!=null) addCookie(response, token, user);
        return user;
    }

    public void addCookie(HttpServletResponse response, String token, SeckillUser user){
        // 每次登录都会生成一个新的session存储于redis，并将改session的id反馈给客户端，一个session对应存储一个user对象
        redisService.set(SeckillUserKeyPrefix.token,token,user);
        // 将token写入cookie中, 然后传给客户端（一个cookie对应一个用户，这里将这个cookie的用户信息写入redis中
        Cookie cookie = new Cookie(COOKIE_NAME_TOKEN,token);//cookie是键值对，其中的value存放着这个redis缓存中的key
        cookie.setMaxAge(SeckillUserKeyPrefix.token.expireSeconds());// 保持与redis中的session一致
        cookie.setPath("/");//Cookie 的 path 属性可以有效的过滤哪些 Cookie 可以发送给服务器，哪些不发。
        response.addCookie(cookie);
    }
}